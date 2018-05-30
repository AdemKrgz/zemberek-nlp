package zemberek.morphology.ambiguity;

import com.google.common.collect.Lists;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.collections.IntValueMap;
import zemberek.core.compression.LossyIntLookup;
import zemberek.core.dynamic.ActiveList;
import zemberek.core.dynamic.Scoreable;
import zemberek.core.io.IOUtil;
import zemberek.core.io.Strings;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.PrimaryPos;
import zemberek.core.turkish.SecondaryPos;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.SingleAnalysis.MorphemeGroup;
import zemberek.morphology.analysis.WordAnalysis;

/**
 * This is a class for applying morphological ambiguity resolution for Turkish sentences. Algorithm
 * is based on "Haşim Sak, Tunga Güngör, and Murat Saraçlar. Morphological disambiguation of Turkish
 * text with perceptron algorithm. In CICLing 2007, volume LNCS 4394, pages 107-118, 2007".
 *
 * @see <a href="http://www.cmpe.boun.edu.tr/~hasim">Haşim Sak</a>
 * <p>
 * This is code is adapted from the Author's original Perl implementation. However, this is not a
 * direct port, many changes needed to be applied for Zemberek integration and cleaner design.
 * <p>
 * For Training, use {@link PerceptronAmbiguityResolverTrainer} class.
 */
public class PerceptronAmbiguityResolver implements AmbiguityResolver {

  Decoder decoder;

  PerceptronAmbiguityResolver(WeightLookup averagedModel, FeatureExtractor extractor) {
    this.decoder = new Decoder(averagedModel, extractor);
  }

  WeightLookup getModel() {
    return decoder.model;
  }

  public static PerceptronAmbiguityResolver fromModelFile(Path modelFile) throws IOException {

    WeightLookup lookup;
    if (CompressedModel.isCompressed(modelFile)) {
      lookup = CompressedModel.deserialize(modelFile);
    } else {
      lookup = Model.loadFromFile(modelFile);
    }
    FeatureExtractor extractor = new FeatureExtractor(false);
    return new PerceptronAmbiguityResolver(lookup, extractor);
  }

  public static PerceptronAmbiguityResolver fromResource(String resourcePath) throws IOException {

    WeightLookup lookup;
    if (CompressedModel.isCompressed(resourcePath)) {
      lookup = CompressedModel.deserialize(resourcePath);
    } else {
      lookup = Model.loadFromResource(resourcePath);
    }
    FeatureExtractor extractor = new FeatureExtractor(false);
    return new PerceptronAmbiguityResolver(lookup, extractor);
  }

  @Override
  public SentenceAnalysis disambiguate(String sentence, List<WordAnalysis> allAnalyses) {
    DecodeResult best = decoder.bestPath(allAnalyses);
    List<SentenceWordAnalysis> l = new ArrayList<>();
    for (int i = 0; i < allAnalyses.size(); i++) {
      WordAnalysis wordAnalysis = allAnalyses.get(i);
      SingleAnalysis analysis = best.bestParse.get(i);
      l.add(new SentenceWordAnalysis(analysis, wordAnalysis));
    }
    return new SentenceAnalysis(sentence, l);
  }

  static class FeatureExtractor {

    boolean useCache;

    ConcurrentHashMap<SingleAnalysis[], IntValueMap<String>> featureCache =
        new ConcurrentHashMap<>();

    FeatureExtractor(boolean useCache) {
      this.useCache = useCache;
    }

    // This is used for training. Extracts feature counts from current best analysis sequence.
    // Trainer then uses this counts to update weights for those features.
    IntValueMap<String> extractFeatureCounts(List<SingleAnalysis> bestSequence) {
      List<SingleAnalysis> seq = Lists.newArrayList(sentenceBegin, sentenceBegin);
      seq.addAll(bestSequence);
      seq.add(sentenceEnd);
      IntValueMap<String> featureCounts = new IntValueMap<>();
      for (int i = 2; i < seq.size(); i++) {
        SingleAnalysis[] trigram = {
            seq.get(i - 2),
            seq.get(i - 1),
            seq.get(i)};
        IntValueMap<String> trigramFeatures = extractFromTrigram(trigram);
        for (IntValueMap.Entry<String> s : trigramFeatures.iterableEntries()) {
          featureCounts.incrementByAmount(s.key, s.count);
        }
      }
      return featureCounts;
    }

    IntValueMap<String> extractFromTrigram(SingleAnalysis[] trigram) {

      if (useCache) {
        IntValueMap<String> cached = featureCache.get(trigram);
        if (cached != null) {
          return cached;
        }
      }

      IntValueMap<String> feats = new IntValueMap<>();
      SingleAnalysis w1 = trigram[0];
      SingleAnalysis w2 = trigram[1];
      SingleAnalysis w3 = trigram[2];

      String r1 = w1.getDictionaryItem().id;
      String r2 = w2.getDictionaryItem().id;
      String r3 = w3.getDictionaryItem().id;

      String ig1 = w1.formatMorphemesLexical();
      String ig2 = w2.formatMorphemesLexical();
      String ig3 = w3.formatMorphemesLexical();

      String r1Ig1 = r1 + "+" + ig1;
      String r2Ig2 = r2 + "+" + ig2;
      String r3Ig3 = r3 + "+" + ig3;

      feats.addOrIncrement("1:" + r1Ig1 + "-" + r2Ig2 + "-" + r3Ig3);
      feats.addOrIncrement("2:" + r1 + ig2 + r3Ig3);
      feats.addOrIncrement("3:" + r2Ig2 + "-" + r3Ig3);
      feats.addOrIncrement("4:" + r3Ig3);
      //feats.addOrIncrement("5:" + r2 + ig2 + "-" + ig3);
      //feats.addOrIncrement("6:" + r1 + ig1 + "-" + ig3);

      //feats.addOrIncrement("7:" + r1 + "-" + r2 + "-" + r3);
      //feats.addOrIncrement("8:" + r1 + "-" + r3);
      feats.addOrIncrement("9:" + r2 + "-" + r3);
      feats.addOrIncrement("10:" + r3);

      //feats.addOrIncrement("11:" + ig1 + "-" + ig2 + "-" + ig3);
      //feats.addOrIncrement("12:" + ig1 + "-" + ig3);
      feats.addOrIncrement("13:" + ig2 + "-" + ig3);
      feats.addOrIncrement("14:" + ig3);

      MorphemeGroup[] lastWordGroups = w3.getGroups();
      String[] lastWordGroupsLex = new String[lastWordGroups.length];
      for (int i = 0; i < lastWordGroupsLex.length; i++) {
        lastWordGroupsLex[i] = lastWordGroups[i].lexicalForm();
      }

      String w1LastGroup = w1.getLastGroup().lexicalForm();
      String w2LastGroup = w2.getLastGroup().lexicalForm();

      for (String ig : lastWordGroupsLex) {
        feats.addOrIncrement("15:" + w1LastGroup + "-" + w2LastGroup + "-" + ig);
        //feats.addOrIncrement("16:" + w1LastGroup + "-" + ig);
        feats.addOrIncrement("17:" + w2LastGroup + ig);
        //feats.addOrIncrement("18:" + ig);
      }

      for (int k = 0; k < lastWordGroupsLex.length - 1; k++) {
        //feats.addOrIncrement("19:" + lastWordGroupsLex[k] + "-" + lastWordGroupsLex[k + 1]);
      }

      for (int k = 0; k < lastWordGroupsLex.length; k++) {
        feats.addOrIncrement("20:" + k + "-" + lastWordGroupsLex[k]);
      }

      if (Character.isUpperCase(r3.charAt(0))
          && w3.getDictionaryItem().secondaryPos == SecondaryPos.ProperNoun) {
        feats.addOrIncrement("21:PROPER");
      }

      feats.addOrIncrement("22:" + w3.groupCount());
      //
      if ((w3 == sentenceEnd || w3.getDictionaryItem().lemma.equals("."))
          && w3.getDictionaryItem().primaryPos == PrimaryPos.Verb) {
        feats.addOrIncrement("23:ENDSVERB");
      }
      if (useCache) {
        featureCache.put(trigram, feats);
      }
      return feats;
    }
  }

  private static final SingleAnalysis sentenceBegin = SingleAnalysis.unknown("<s>");
  private static final SingleAnalysis sentenceEnd = SingleAnalysis.unknown("</s>");

  /**
   * Decoder finds the best path from multiple word analyses using Viterbi search algorithm.
   */
  static class Decoder {

    WeightLookup model;
    FeatureExtractor extractor;

    Decoder(WeightLookup model,
        FeatureExtractor extractor) {
      this.model = model;
      this.extractor = extractor;
    }

    DecodeResult bestPath(List<WordAnalysis> sentence) {

      if (sentence.size() == 0) {
        throw new IllegalArgumentException("bestPath cannot be called with empty sentence.");
      }

      // holds the current active paths. initially it contains a single empty Hypothesis.
      ActiveList<Hypothesis> currentList = new ActiveList<>();
      currentList.add(new Hypothesis(sentenceBegin, sentenceBegin, null, 0));

      for (WordAnalysis analysisData : sentence) {

        ActiveList<Hypothesis> nextList = new ActiveList<>();

        // this is necessary because word analysis may contain zero SingleAnalysis
        // So we add an unknown SingleAnalysis to it.
        List<SingleAnalysis> analyses = analysisData.getAnalysisResults();
        if (analyses.size() == 0) {
          analyses = new ArrayList<>(1);
          analyses.add(SingleAnalysis.unknown(analysisData.getInput()));
        }

        for (SingleAnalysis analysis : analyses) {

          for (Hypothesis h : currentList) {

            SingleAnalysis[] trigram = {h.prev, h.current, analysis};
            IntValueMap<String> features = extractor.extractFromTrigram(trigram);

            float trigramScore = 0;
            for (String key : features) {
              trigramScore += (model.get(key) * features.get(key));
            }

            Hypothesis newHyp = new Hypothesis(
                h.current,
                analysis,
                h,
                h.score + trigramScore);
            nextList.add(newHyp);
          }
        }
        currentList = nextList;
      }

      // score for sentence end. No need to create new hypotheses.
      for (Hypothesis h : currentList) {
        SingleAnalysis[] trigram = {h.prev, h.current, sentenceEnd};
        IntValueMap<String> features = extractor.extractFromTrigram(trigram);

        float trigramScore = 0;
        for (String key : features) {
          trigramScore += (model.get(key) * features.get(key));
        }
        h.score += trigramScore;
      }

      Hypothesis best = currentList.getBest();
      float bestScore = best.score;
      List<SingleAnalysis> result = Lists.newArrayList();

      // backtrack. from end to begin, we add words from Hypotheses.
      while (best.previous != null) {
        result.add(best.current);
        best = best.previous;
      }

      // because we collect from end to begin, reverse is required.
      Collections.reverse(result);
      return new DecodeResult(result, bestScore);
    }
  }

  static class DecodeResult {

    List<SingleAnalysis> bestParse;
    float score;

    private DecodeResult(List<SingleAnalysis> bestParse, float score) {
      this.bestParse = bestParse;
      this.score = score;
    }
  }

  static class Hypothesis implements Scoreable {

    SingleAnalysis prev; // previous word analysis result String
    SingleAnalysis current; // current word analysis result String
    Hypothesis previous; // previous Hypothesis.
    float score;

    Hypothesis(
        SingleAnalysis prev,
        SingleAnalysis current,
        Hypothesis previous,
        float score) {
      this.prev = prev;
      this.current = current;
      this.previous = previous;
      this.score = score;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Hypothesis that = (Hypothesis) o;

      if (!prev.equals(that.prev)) {
        return false;
      }
      return current.equals(that.current);
    }

    @Override
    public int hashCode() {
      int result = prev.hashCode();
      result = 31 * result + current.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "Hypothesis{" +
          "prev='" + prev + '\'' +
          ", current='" + current + '\'' +
          ", score=" + score +
          '}';
    }

    @Override
    public float getScore() {
      return score;
    }
  }

  static class CompressedModel implements WeightLookup {

    LossyIntLookup lookup;

    public CompressedModel(LossyIntLookup lookup) {
      this.lookup = lookup;
    }

    @Override
    public float get(String key) {
      return lookup.getAsFloat(key);
    }

    @Override
    public int size() {
      return lookup.size();
    }

    void serialize(Path path) throws IOException {
      lookup.serialize(path);
    }

    static CompressedModel deserialize(Path path) throws IOException {
      LossyIntLookup lookup = LossyIntLookup.deserialize(IOUtil.getDataInputStream(path));
      return new CompressedModel(lookup);
    }

    static boolean isCompressed(DataInputStream dis) throws IOException {
      return LossyIntLookup.checkStream(dis);
    }

    static boolean isCompressed(Path path) throws IOException {
      try (DataInputStream dis = IOUtil.getDataInputStream(path)) {
        return isCompressed(dis);
      }
    }

    static boolean isCompressed(String resource) throws IOException {
      try (DataInputStream dis = IOUtil.getDataInputStream(resource)) {
        return isCompressed(dis);
      }
    }


    static CompressedModel deserialize(String resource) throws IOException {
      try (DataInputStream dis = IOUtil.getDataInputStream(resource)) {
        return new CompressedModel(LossyIntLookup.deserialize(dis));
      }
    }
  }

  interface WeightLookup {

    float get(String key);

    int size();

  }

  static class Model implements WeightLookup, Iterable<String> {

    static float epsilon = 0.0001f;

    FloatValueMap<String> data;

    Model(FloatValueMap<String> data) {
      this.data = data;
    }

    Model() {
      data = new FloatValueMap<>(10000);
    }

    public int size() {
      return data.size();
    }

    static Model loadFromResource(String resource) throws IOException {
      List<String> lines = TextIO.loadLinesFromResource(resource);
      return loadFromLines(lines);
    }

    static Model loadFromFile(Path file) throws IOException {
      List<String> all = TextIO.loadLines(file);
      return loadFromLines(all);
    }

    static Model loadFromLines(List<String> lines) {
      FloatValueMap<String> data = new FloatValueMap<>(10000);
      for (String s : lines) {
        float weight = Float.parseFloat(Strings.subStringUntilFirst(s, " "));
        String key = Strings.subStringAfterFirst(s, " ");
        data.set(key, weight);
      }
      Log.info("Model Loaded.");
      return new Model(data);
    }

    void saveAsText(Path file) throws IOException {
      try (PrintWriter pw = new PrintWriter(file.toFile(), "utf-8")) {
        for (String s : data.getKeyList()) {
          pw.println(data.get(s) + " " + s);
        }
      }
    }

    void pruneNearZeroWeights() {
      FloatValueMap<String> pruned = new FloatValueMap<>();

      for (String key : data) {
        float w = data.get(key);
        if (Math.abs(w) > epsilon) {
          pruned.set(key, w);
        }
      }
      this.data = pruned;
    }

    LossyIntLookup compress() {
      return LossyIntLookup.generate(data);
    }

    public float get(String key) {
      return data.get(key);
    }

    void put(String key, float value) {
      this.data.set(key, value);
    }

    void increment(String key, float value) {
      data.incrementByAmount(key, value);
    }

    @Override
    public Iterator<String> iterator() {
      return data.iterator();
    }

  }

}
