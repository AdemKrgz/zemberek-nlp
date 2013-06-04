package zemberek.lm.compression;

import zemberek.lm.LmVocabulary;
import zemberek.core.hash.LargeNgramMphf;
import zemberek.core.hash.Mphf;
import zemberek.core.hash.MultiLevelMphf;
import zemberek.core.quantization.DoubleLookup;

import java.io.*;
import java.util.logging.Logger;

/**
 * SmoothLm is a compressed, optionally quantized, randomized back-off n-gram language model.
 * It uses Minimal Perfect Hash functions for compression, This means actual n-gram values are not stored in the model.
 * This is a lossy model because for non existing n-grams it may return an existing n-gram probability value.
 * Probability of this happening depends on the fingerprint hash length. This value is determined during the model creation.
 * Regularly 8,16 or 24 bit fingerprints are used and false positive probability for an non existing n-gram is
 * 1/(2^fingerprint bit size).
 * SmoothLm also provides quantization for even more compactness. So probability and back-off values can be quantized to
 * 8, 16 or 24 bits.
 */
public class SmoothLm {

    Logger logger = Logger.getLogger("SmoothLm");

    public static final double DEFAULT_LOG_BASE = 10;
    public static final double DEFAULT_UNIGRAM_WEIGHT = 1;
    public static final double DEFAULT_UNKNOWN_BACKOFF_PENALTY = 0;
    public static final double DEFAULT_STUPID_BACKOFF_ALPHA = 0.4;

    private final int version;
    private final int order;
    private final Mphf[] mphfs;

    private final DoubleLookup[] probabilityLookups;
    private final DoubleLookup[] backoffLookups;
    private final GramDataArray[] ngramData;

    private double[] unigramProbs;
    private double[] unigramBackoffs;

    int[] counts;

    MphfType type;

    private final LmVocabulary vocabulary;

    public static final double LOG_ZERO = -Math.log(Double.MAX_VALUE);

    private double logBase;
    private double unigramWeight;
    private double unknownBackoffPenalty;
    private boolean useStupidBackoff = false;
    private double stupidBackoffLogAlpha;
    private double stupidBackoffAlpha;

    public static class Builder {
        private double _logBase = DEFAULT_LOG_BASE;
        private double _unknownBackoffPenalty = DEFAULT_UNKNOWN_BACKOFF_PENALTY;
        private double _unigramWeight = DEFAULT_UNIGRAM_WEIGHT;
        private boolean _useStupidBackoff = false;
        private double _stupidBackoffAlpha = DEFAULT_STUPID_BACKOFF_ALPHA;
        private DataInputStream _dis;

        public Builder(InputStream is) {
            this._dis = new DataInputStream(new BufferedInputStream(is));
        }

        public Builder(File file) throws FileNotFoundException {
            this._dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
        }

        public Builder logBase(double logBase) {
            this._logBase = logBase;
            return this;
        }

        public Builder unknownBackoffPenalty(double unknownPenalty) {
            this._unknownBackoffPenalty = unknownPenalty;
            return this;
        }

        public Builder unigramWeight(double weight) {
            this._unigramWeight = weight;
            return this;
        }

        public Builder useStupidBackoff() {
            this._useStupidBackoff = true;
            return this;
        }

        public Builder stupidBackoffAlpha(double alphaValue) {
            this._stupidBackoffAlpha = alphaValue;
            return this;
        }

        public SmoothLm build() throws IOException {
            return new SmoothLm(
                    _dis,
                    _logBase,
                    _unigramWeight,
                    _unknownBackoffPenalty,
                    _useStupidBackoff,
                    _stupidBackoffAlpha);
        }
    }

    public static Builder builder(InputStream is) throws IOException {
        return new Builder(is);
    }

    public static Builder builder(File modelFile) throws IOException {
        return new Builder(modelFile);
    }

    private SmoothLm(
            DataInputStream dis,
            double logBase,
            double unigramWeight,
            double unknownBackoffPenalty,
            boolean useStupidBackoff,
            double stupidBackoffAlpha) throws IOException {
        this(dis); // load the lm data.
        // Now apply necessary transformations and configurations
        this.unigramWeight = unigramWeight;
        this.unknownBackoffPenalty = unknownBackoffPenalty;
        this.useStupidBackoff = useStupidBackoff;
        this.stupidBackoffAlpha = stupidBackoffAlpha;

        if (logBase != DEFAULT_LOG_BASE) {
            logger.info("Changing log base from " + DEFAULT_LOG_BASE + " to " + logBase);
            changeLogBase(logBase);
            this.stupidBackoffLogAlpha = Math.log(stupidBackoffAlpha) / Math.log(logBase);
        } else {
            this.stupidBackoffLogAlpha = Math.log(stupidBackoffAlpha) / Math.log(DEFAULT_LOG_BASE);
        }

        this.logBase = logBase;

        if (unigramWeight != DEFAULT_UNIGRAM_WEIGHT) {
            logger.info("Applying unigram smoothing with unigram weight: " + unigramWeight);
            applyUnigramSmoothing(unigramWeight);
        }

        if (useStupidBackoff) {
            logger.info("Lm will use stupid back off with alpha value: " + stupidBackoffAlpha);
        }
    }

    public String info() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Order: %d%n", order));

        for (int i = 1; i < ngramData.length; i++) {
            GramDataArray gramDataArray = ngramData[i];
            sb.append(String.format("%d Gram Count: %d%n", i, gramDataArray.count));
        }

        sb.append(String.format("Log Base: %.2f%n", logBase));
        sb.append(String.format("Unigram Weight: %.2f%n", unigramWeight));
        sb.append(String.format("Using Stupid Back-off?: %s%n", useStupidBackoff ? "Yes" : "No"));
        if (useStupidBackoff)
            sb.append(String.format("Stupid Back-off Alpha Value: %.2f%n", stupidBackoffAlpha));
        sb.append(String.format("Unknown Backoff N-gram penalty: %.2f%n", unknownBackoffPenalty));
        return sb.toString();
    }

    public static enum MphfType {
        SMALL, LARGE
    }

    public int getVersion() {
        return version;
    }

    public int getOrder() {
        return order;
    }

    public LmVocabulary getVocabulary() {
        return vocabulary;
    }

    public int getGramCount(int n) {
        return ngramData[n].count;
    }

    private SmoothLm(DataInputStream dis) throws IOException {
        this.version = dis.readInt();
        int typeInt = dis.readInt();
        if (typeInt == 0)
            type = MphfType.SMALL;
        else
            type = MphfType.LARGE;

        this.logBase = dis.readDouble();
        this.order = dis.readInt();

        counts = new int[order + 1];
        for (int i = 1; i <= order; i++) {
            counts[i] = dis.readInt();
        }

        // probability lookups
        probabilityLookups = new DoubleLookup[order + 1];
        for (int i = 1; i <= order; i++) {
            probabilityLookups[i] = DoubleLookup.getLookup(dis);
        }
        // backoff lookups
        backoffLookups = new DoubleLookup[order + 1];
        for (int i = 1; i < order; i++) {
            backoffLookups[i] = DoubleLookup.getLookup(dis);
        }

        //load fingerprint, probability and backoff data.
        ngramData = new GramDataArray[order + 1];
        for (int i = 1; i <= order; i++) {
            ngramData[i] = new GramDataArray(dis);
        }

        // we take the unigram probability data out to get rid of rank lookups for speed.
        unigramProbs = new double[ngramData[1].count];
        unigramBackoffs = new double[ngramData[1].count];
        for (int i = 0; i < ngramData[1].count; i++) {
            final int probability = ngramData[1].getProbabilityRank(i);
            unigramProbs[i] = probabilityLookups[1].get(probability);
            final int backoff = ngramData[1].getBackoffRank(i);
            unigramBackoffs[i] = backoffLookups[1].get(backoff);
        }

        // load MPHFs
        if (type == MphfType.LARGE) {
            mphfs = new LargeNgramMphf[order + 1];
            for (int i = 2; i <= order; i++) {
                mphfs[i] = LargeNgramMphf.deserialize(dis);
            }
        } else {
            mphfs = new MultiLevelMphf[order + 1];
            for (int i = 2; i <= order; i++) {
                mphfs[i] = MultiLevelMphf.deserialize(dis);
            }
        }

        // load vocabulary
        int vocabularySize = dis.readInt();
        vocabulary = new LmVocabulary(dis, vocabularySize);

        dis.close();
    }

    /**
     * Retrieves the dequantized log probability of an n-gram.
     *
     * @param tokenIds token-id sequence. A token id is the unigram index value.
     * @return dequantized log probability of the n-gram. if n-gram does not exist, it returns 0
     * @throws IllegalArgumentException if tokenId sequence length is zero or more than n value.
     */
    public double getProbabilityValue(int... tokenIds) {

        final int ng = tokenIds.length;
        if (ng == 1) {
            return unigramProbs[tokenIds[0]];
        }

        int quickHash = MultiLevelMphf.hash(tokenIds, -1);

        int index = mphfs[ng].get(tokenIds, quickHash);

        if (ngramData[ng].checkFingerPrint(quickHash, index))
            return probabilityLookups[ng].get(ngramData[ng].getProbabilityRank(index));
        else {
            return 0;
        }
    }

    /**
     * Retrieves the dequantized backoff value of an n-gram.
     *
     * @param tokenIds token-id sequence. A token id is the unigram index value.
     * @return dequantized log back-off of the n-gram. if n-gram does not exist, it returns Double.NEGATIVE_INFINITY
     * @throws IllegalArgumentException if tokenId sequence length is zero or more than n value.
     */
    public double getBackoffValue(int... tokenIds) {
        if (useStupidBackoff)
            return stupidBackoffLogAlpha;
        final int ng = tokenIds.length;
        if (ng == 1) {
            return unigramBackoffs[tokenIds[0]];
        }
        final int quickHash = MultiLevelMphf.hash(tokenIds, -1);

        final int index = mphfs[ng].get(tokenIds, quickHash);

        if (ngramData[ng].checkFingerPrint(quickHash, index))
            return backoffLookups[ng].get(ngramData[ng].getBackoffRank(index));
        else {
            return unknownBackoffPenalty;
        }
    }

    /**
     * Calculates the dequantized probability value for an n-gram. If n-gram does not exist, it applies
     * backoff calculation.
     *
     * @param tokenIds token-id sequence. A token id is the unigram index value.
     * @return dequantized log backoff value of the n-gram. if there is no backoff value or n-gram does not exist,
     *         it returns LOG_ZERO. This mostly happens in the condition that words queried does not exist
     *         in the vocabulary.
     * @throws IllegalArgumentException if tokenId sequence length is zero or more than n value.
     */
    double getProbabilityRecursive(int... tokenIds) {

        if (tokenIds.length == 0 || tokenIds.length > order)
            throw new IllegalArgumentException(
                    "At least one or max Gram Count" + order + " tokens are required. But it is:" + tokenIds.length);

        double result = 0;
        double probability = getProbabilityValue(tokenIds);
        if (probability == 0) { // if probability does not exist.
            if (tokenIds.length == 1)
                return LOG_ZERO;
            double backoffValue = useStupidBackoff ? stupidBackoffLogAlpha : getBackoffValue(head(tokenIds));
            result = result + backoffValue + getProbabilityRecursive(tail(tokenIds));
        } else {
            result = probability;
        }
        return result;
    }

    private static final int BIT_MASK21 = (1 << 21) - 1;

    /**
     * Returns the log probability value of an encoded Trigram. Trigram is embedded into an 64 bit long value as:
     * [1bit empty][21 bits gram-3][21 bits gram-2][21 bits gram-1]
     *
     * @param encodedTrigram encoded trigram
     * @return log probability.
     */
    public double getEncodedTrigramProbability(long encodedTrigram) {
        int fingerPrint = MultiLevelMphf.hash(encodedTrigram, 3, -1);
        int index = mphfs[3].get(encodedTrigram, 3, fingerPrint);
        double result = 0;
        if (ngramData[3].checkFingerPrint(fingerPrint, index)) { // if p(c|a,b) exist
            return probabilityLookups[3].get(ngramData[3].getProbabilityRank(index));
        } else { // we back off to two grams. p(c|a,b) ~ b(a,b) + p(c|b)
            if (useStupidBackoff)
                result += stupidBackoffLogAlpha;
            else {
                fingerPrint = MultiLevelMphf.hash(encodedTrigram, 2, -1);
                index = mphfs[2].get(encodedTrigram, 2, fingerPrint);
                if (ngramData[2].checkFingerPrint(fingerPrint, index)) { // if backoff (a,b) exist
                    result += backoffLookups[2].get(ngramData[2].getBackoffRank(index));
                } else result += unknownBackoffPenalty;
            }
            long encodedBigram = encodedTrigram >>> 21;
            fingerPrint = MultiLevelMphf.hash(encodedBigram, 2, -1);
            index = mphfs[2].get(encodedBigram, 2, fingerPrint);
            if (ngramData[2].checkFingerPrint(fingerPrint, index)) { // if p(b|c) exists
                return result + probabilityLookups[2].get(ngramData[2].getProbabilityRank(index));
            } else { // p(b|c) ~ b(b) + p(c)
                result += unigramProbs[((int) (encodedBigram >> 21))];
                if (useStupidBackoff)
                    return result + stupidBackoffLogAlpha;
                return result + unigramBackoffs[((int) (encodedBigram & BIT_MASK21))];
            }
        }
    }

    private int[] head(int[] arr) {
        if (arr.length == 1)
            return new int[0];
        int[] head = new int[arr.length - 1];
        System.arraycopy(arr, 0, head, 0, arr.length - 1);
        return head;
    }

    private int[] tail(int[] arr) {
        if (arr.length == 1)
            return new int[0];
        int[] head = new int[arr.length - 1];
        System.arraycopy(arr, 1, head, 0, arr.length - 1);
        return head;
    }

    /**
     * This is the non recursive log probability calculation. It is more complicated but faster.
     *
     * @param tokens token array
     * @return log probability.
     */
    public double getProbability(String... tokens) {
        return getProbability(vocabulary.getIds(tokens));
    }

    /**
     * This is the non recursive log probability calculation. It is more complicated but faster.
     *
     * @param tokenIds token id array
     * @return log probability.
     */
    public double getProbability(int... tokenIds) {
        int n = tokenIds.length;

        if (n == 0 || n > order)
            throw new IllegalArgumentException(
                    "At least one or max Gram Count" + order + " tokens are required. But it is:" + tokenIds.length);
        if (n == 1)
            return unigramProbs[tokenIds[0]];
        if (n == 2) {
            double prob = getProbabilityValue(tokenIds);
            if (prob == 0) {
                return unigramBackoffs[tokenIds[0]] + unigramProbs[tokenIds[1]];
            } else {
                return prob;
            }
        }
        int begin = 0;
        double result = 0;
        int gram = n;
        while (gram > 1) {
            // try to find P(N|begin..N-1)
            int quickHash = MultiLevelMphf.hash(tokenIds, begin, n, -1);
            int index = mphfs[gram].get(tokenIds, begin, n, quickHash);
            if (!ngramData[gram].checkFingerPrint(quickHash, index)) { // if there is no probability value, back off to B(begin..N-1)
                if (useStupidBackoff) {
                    if (gram == 2)
                        return result + unigramProbs[tokenIds[n - 1]] + stupidBackoffLogAlpha;
                    else
                        result += stupidBackoffLogAlpha;
                } else {
                    // we are already backed off to unigrams because no bigram found. So we return only P(N)+B(N-1)
                    if (gram == 2) {
                        return result + unigramProbs[tokenIds[n - 1]] + unigramBackoffs[tokenIds[begin]];
                    }
                    quickHash = MultiLevelMphf.hash(tokenIds, begin, n - 1, -1);
                    index = mphfs[gram - 1].get(tokenIds, begin, n - 1, quickHash);
                    if (ngramData[gram - 1].checkFingerPrint(quickHash, index)) { //if backoff available, we add it to resutlt.
                        result += backoffLookups[gram - 1].get(ngramData[gram - 1].getBackoffRank(index));
                    } else
                        result += unknownBackoffPenalty;
                }
            } else {
                // we have found the P(N|begin..N-1) we return the accumulated result.
                return result + probabilityLookups[gram].get(ngramData[gram].getProbabilityRank(index));
            }
            begin++;
            gram = n - begin;
        }
        return result;
    }

    /**
     * This method is used when calculating probability of an ngram sequence, how many times it backed off to lower order
     * n-gram calculations.
     *
     * @param tokens n-gram strings
     * @return if no back-off, returns 0 if none of the n-grams exist (Except 1 gram), it returns order-1
     */
    public int getBackoffCount(String... tokens) {
        return getBackoffCount(vocabulary.getIds(tokens));
    }

    /**
     * This method is used when calculating probability of an ngram sequence, how many times it backed off to lower order
     * n-gram calculations.
     *
     * @param tokenIds n-gram index array
     * @return if no back-off, returns 0 if none of the n-grams exist (Except 1 gram), it returns order-1
     */
    public int getBackoffCount(int... tokenIds) {
        int n = tokenIds.length;
        if (n == 0 || n > order)
            throw new IllegalArgumentException(
                    "At least one or " + order + " tokens are required. But it is:" + tokenIds.length);
        if (n == 1) return 0;
        if (n == 2) return getProbabilityValue(tokenIds) == 0 ? 1 : 0;

        int begin = 0;
        int backoffCount = 0;
        int gram = n;
        while (gram > 1) {
            // try to find P(N|begin..N-1)
            int quickHash = MultiLevelMphf.hash(tokenIds, begin, n, -1);
            int index = mphfs[gram].get(tokenIds, begin, n, quickHash);
            if (!ngramData[gram].checkFingerPrint(quickHash, index)) { //  back off to B(begin..N-1)
                backoffCount++;
            } else {
                return backoffCount;
            }
            begin++;
            gram = n - begin;
        }
        return backoffCount;
    }

    public String getPropabilityExpression(int... ids) {
        int last = ids[ids.length - 1];
        StringBuilder sb = new StringBuilder("p(" + vocabulary.getWord(last));
        if (ids.length > 1)
            sb.append("|");
        for (int j = 0; j < ids.length - 1; j++) {
            sb.append(vocabulary.getWord(ids[j]));
            if (j < ids.length - 2)
                sb.append(",");
        }
        sb.append(")");
        return sb.toString();
    }

    public String getBackoffExpression(int... ids) {
        StringBuilder sb = new StringBuilder("BO(");
        for (int j = 0; j < ids.length; j++) {
            sb.append(vocabulary.getWord(ids[j]));
            if (j < ids.length - 1)
                sb.append(",");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * It generates a single line String that explains the probability calculations.
     *
     * @param tokenIds n-gram index array
     * @return explanation String.
     */
    public String explain(int... tokenIds) {
        return explain(new Explanation(), tokenIds).sb.toString();
    }

    private Explanation explain(Explanation exp, int... tokenIds) {
        double probability = getProbabilityValue(tokenIds);
        exp.sb.append(getPropabilityExpression(tokenIds));
        if (probability == 0) { // if probability does not exist.
            exp.sb.append("=[");
            double backOffValue = getBackoffValue(head(tokenIds));
            String backOffStr = getBackoffExpression(head(tokenIds));
            exp.sb.append(backOffStr).append("=").append(fmt(backOffValue)).append(" + ");
            exp.score = exp.score + backOffValue + explain(exp, tail(tokenIds)).score;
            exp.sb.append("]");
        } else {
            exp.score = probability;
        }
        exp.sb.append("= ").append(fmt(exp.score));
        return exp;
    }

    private static class Explanation {
        StringBuilder sb = new StringBuilder();
        double score = 0;
    }


    private String fmt(double d) {
        return String.format("%.3f", d);
    }

    /**
     * Changes the log base. Generally probabilities in language models are log10 based. But some applications uses their
     * own log base. Such as Sphinx uses 1.0003 as their base, Makine decoder uses e.
     *
     * @param newBase new logBase
     */
    public void changeLogBase(double newBase) {
        DoubleLookup.changeBase(unigramProbs, logBase, newBase);
        DoubleLookup.changeBase(unigramBackoffs, logBase, newBase);
        for (int i = 2; i < probabilityLookups.length; i++) {
            probabilityLookups[i].changeBase(logBase, newBase);
            if (i < probabilityLookups.length - 1)
                backoffLookups[i].changeBase(logBase, newBase);
        }
        this.logBase = newBase;
    }

    /**
     * This method applies more smoothing to unigram log probability values. Some ASR engines does this.
     *
     * @param unigramWeight weight factor.
     */
    private void applyUnigramSmoothing(double unigramWeight) {
        double logUnigramWeigth = Math.log(unigramWeight);
        double inverseLogUnigramWeigth = Math.log(1 - unigramWeight);
        double logUniformUnigramProbability = -Math.log(unigramProbs.length);
        // apply uni-gram weight. This applies smoothing to unigrams. As lowering high probabilities and
        // adding gain to small probabilities.
        // uw = uni-gram weight  , uniformProb = 1/#unigram
        // so in linear domain, we apply this to all probability values as: p(w1)*uw + uniformProb * (1-uw) to
        // maintain the probability total is one while smoothing the values.
        // this converts to log(p(w1)*uw + uniformProb*(1-uw) ) which is calculated with log probabilities
        // a = log(p(w1)) + log(uw) and b = -log(#unigram)+log(1-uw) applying logsum(a,b)
        // approach is taken from Sphinx-4
        for (int i = 0; i < unigramProbs.length; i++) {
            double p1 = unigramProbs[i] + logUnigramWeigth;
            double p2 = logUniformUnigramProbability + inverseLogUnigramWeigth;
            unigramProbs[i] = logSumExact(p1, p2);
        }
    }

    /**
     * Exact calculation of log(a+b) using log(a) and log(b) with formula
     * <p><b>log(a+b) = log(b) + log(1 + exp(log(b)-log(a)))</b> where log(b)>log(a)
     *
     * @param logA logarithm of A
     * @param logB logarithm of B
     * @return approximation of log(A+B)
     */
    private static double logSumExact(double logA, double logB) {
        if (Double.isInfinite(logA))
            return logB;
        if (Double.isInfinite(logB))
            return logA;
        if (logA > logB) {
            double dif = logA - logB;
            return dif >= 30d ? logA : logA + Math.log(1 + Math.exp(-dif));
        } else {
            double dif = logB - logA;
            return dif >= 30d ? logB : logB + Math.log(1 + Math.exp(-dif));
        }
    }

    /**
     * Returns the logarithm base of the values used in this model.
     *
     * @return log base
     */
    public double getLogBase() {
        return logBase;
    }

    private static class CacheEntry {
        int fp;
        int[] key;
        double probability;
        double backoff;
    }

    private static class Cache {
        CacheEntry entries[];
        final int mask;

        public Cache(int i) {
            entries = new CacheEntry[i];
            mask = i - 1;
        }

        public CacheEntry check(int qhash) {
            return entries[qhash & mask];
        }

        public void add(int qhash, int[] keys, double prob, double backoff) {

        }
    }
}
