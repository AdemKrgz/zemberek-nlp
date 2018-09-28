package zemberek.normalization;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.antlr.v4.runtime.Token;
import zemberek.core.dynamic.ActiveList;
import zemberek.core.dynamic.Scorable;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.SecondaryPos;
import zemberek.core.turkish.TurkishAlphabet;
import zemberek.lm.compression.SmoothLm;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.generator.WordGenerator;
import zemberek.morphology.morphotactics.Morpheme;
import zemberek.normalization.deasciifier.Deasciifier;
import zemberek.tokenization.TurkishTokenizer;
import zemberek.tokenization.antlr.TurkishLexer;

/**
 * Tries to normalize a sentence using lookup tables and heuristics.
 */
public class TurkishSentenceNormalizer {

  TurkishMorphology morphology;
  Map<String, String> commonSplits = new HashMap<>();
  SmoothLm lm;
  TurkishSpellChecker spellChecker;
  HashSet<String> commonConnectedSuffixes = new HashSet<>();

  public TurkishSentenceNormalizer(
      TurkishMorphology morphology,
      Path commonSplit,
      SmoothLm languageModel) throws IOException {
    Log.info("Language model = %s", languageModel.info());
    this.morphology = morphology;

    List<String> splitLines = Files.readAllLines(commonSplit, Charsets.UTF_8);
    for (String splitLine : splitLines) {
      String[] tokens = splitLine.split("=");
      commonSplits.put(tokens[0].trim(), tokens[1].trim());
    }
    this.lm = languageModel;

    StemEndingGraph graph = new StemEndingGraph(morphology);
    CharacterGraphDecoder decoder = new CharacterGraphDecoder(graph.stemGraph);
    this.spellChecker = new TurkishSpellChecker(
        morphology,
        decoder,
        CharacterGraphDecoder.ASCII_TOLERANT_MATCHER);
    this.commonConnectedSuffixes.addAll(TextIO.loadLinesFromResource("question-suffixes"));
    this.commonConnectedSuffixes.addAll(Arrays.asList("de", "da", "ki"));
  }

  public String normalize(List<Token> tokens) {
    String s = combineNecessaryWords(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = splitNecessaryWords(tokens, false);
    if (probablyRequiresDeasciifier(s)) {
      Deasciifier deasciifier = new Deasciifier(s);
      s = deasciifier.convertToTurkish();
    }
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = combineNecessaryWords(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = splitNecessaryWords(tokens, true);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);

    s = useInformalAnalysis(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = useSpellChecker(tokens);
    return s;
  }

  public String normalize(String input) {
    List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(input);
    return normalize(tokens);
  }

  String splitNecessaryWords(List<Token> tokens, boolean useLookup) {
    List<String> result = new ArrayList<>();
    for (Token token : tokens) {
      String text = token.getText();
      if (isWord(token)) {
        result.add(separateCommon(text, useLookup));
      } else {
        result.add(text);
      }
    }
    return String.join(" ", result);
  }

  boolean isWord(Token token) {
    int type = token.getType();
    return type == TurkishLexer.Word
        || type == TurkishLexer.UnknownWord
        || type == TurkishLexer.WordAlphanumerical
        || type == TurkishLexer.WordWithSymbol;
  }

  String combineNecessaryWords(List<Token> tokens) {
    List<String> result = new ArrayList<>();
    boolean combined = false;
    for (int i = 0; i < tokens.size() - 1; i++) {
      Token first = tokens.get(i);
      Token second = tokens.get(i + 1);
      String firstS = first.getText();
      String secondS = second.getText();
      if (!isWord(first) || !isWord(second)) {
        combined = false;
        result.add(firstS);
        continue;
      }
      if (combined) {
        combined = false;
        continue;
      }
      String c = combineCommon(firstS, secondS);
      if (c.length() > 0) {
        result.add(c);
        combined = true;
      } else {
        result.add(first.getText());
        combined = false;
      }
    }
    if (!combined) {
      result.add(tokens.get(tokens.size() - 1).getText());
    }
    return String.join(" ", result);
  }

  String useInformalAnalysis(List<Token> tokens) {
    List<String> result = new ArrayList<>();
    for (Token token : tokens) {
      String text = token.getText();
      if (isWord(token)) {
        WordAnalysis a = morphology.analyze(text);

        if (a.analysisCount() > 0) {

          if (a.stream().anyMatch(s -> !s.containsInformalMorpheme())) {
            result.add(text);
          } else {
            SingleAnalysis s = a.getAnalysisResults().get(0);
            WordGenerator gen = morphology.getWordGenerator();
            List<Morpheme> generated = toFormalMorphemeNames(s);
            List<WordGenerator.Result> results = gen.generate(s.getDictionaryItem(), generated);
            if (results.size() > 0) {
              result.add(results.get(0).surface);
            } else {
              result.add(text);
            }
          }
        } else {
          result.add(text);
        }
      } else {
        result.add(text);
      }
    }
    return String.join(" ", result);
  }

  List<Morpheme> toFormalMorphemeNames(SingleAnalysis a) {
    List<Morpheme> transform = new ArrayList<>();
    for (Morpheme m : a.getMorphemes()) {
      if (m.informal && m.mappedMorpheme != null) {
        transform.add(m.mappedMorpheme);
      } else {
        transform.add(m);
      }
    }
    return transform;
  }

  String useSpellChecker(List<Token> tokens) {

    List<String> result = new ArrayList<>();
    for (int i = 0; i < tokens.size(); i++) {
      Token currentToken = tokens.get(i);
      String current = currentToken.getText();
      String next = i == tokens.size() - 1 ? null : tokens.get(i + 1).getText();
      String previous = i == 0 ? null : tokens.get(i - 1).getText();
      if (isWord(currentToken) && (!hasAnalysis(current))) {
        List<String> candidates = spellChecker.suggestForWord(current, previous, next, lm);
        if (candidates.size() > 0) {
          result.add(candidates.get(0));
        } else {
          result.add(current);
        }
      } else {
        result.add(current);
      }
    }
    return String.join(" ", result);
  }

  List<String> getSpellCandidates(Token currentToken, String previous, String next) {
    String current = currentToken.getText();
    if (isWord(currentToken) && (!hasAnalysis(current))) {
      List<String> candidates = spellChecker.suggestForWord(current, previous, next, lm);
      if (candidates.size() > 0) {
        return candidates;
      }
    }
    return Collections.emptyList();
  }


  /**
   * Tries to combine words that are written separately using heuristics. If it cannot combine,
   * returns empty string.
   *
   * Such as:
   * <pre>
   * göndere bilirler -> göndere bilirler
   * elma lar -> elmalar
   * ankara 'ya -> ankara'ya
   * </pre>
   */
  String combineCommon(String i1, String i2) {
    String combined = i1 + i2;
    if (i2.startsWith("'") || i2.startsWith("bil")) {
      if (hasAnalysis(combined)) {
        return combined;
      }
    }
    if (!hasRegularAnalysis(i2)) {
      if (hasAnalysis(combined)) {
        return combined;
      }
    }
    return "";
  }

  boolean hasAnalysis(String s) {
    WordAnalysis a = morphology.analyze(s);
    return a.analysisCount() > 0;
  }

  /**
   * Returns true if only word is analysed with internal dictionary and analysis dictionary item is
   * not proper noun.
   */
  boolean hasRegularAnalysis(String s) {
    WordAnalysis a = morphology.analyze(s);
    return a.stream().anyMatch(k -> !k.isUnknown() && !k.isRuntime() &&
        k.getDictionaryItem().secondaryPos != SecondaryPos.ProperNoun &&
        k.getDictionaryItem().secondaryPos != SecondaryPos.Abbreviation
    );
  }

  /**
   * Tries to separate question words, conjunctions and common mistakes by looking from a lookup or
   * using heuristics. Such as:
   * <pre>
   * gelecekmisin -> gelecek misin
   * tutupda -> tutup da
   * öyleki -> öyle ki
   * olurya -> olur ya
   * </pre>
   */
  String separateCommon(String input, boolean useLookup) {
    if (useLookup && commonSplits.containsKey(input)) {
      return commonSplits.get(input);
    }
    if (!hasRegularAnalysis(input)) {
      for (int i = 1; i < input.length() - 1; i++) {
        String tail = input.substring(i);
        if (commonConnectedSuffixes.contains(tail)) {
          String head = input.substring(0, i);
          if (hasRegularAnalysis(head)) {
            return head + " " + tail;
          } else {
            return input;
          }
        }
      }
    }
    return input;
  }

  /**
   * Makes a guess if input sentence requires deasciifier.
   */
  static boolean probablyRequiresDeasciifier(String sentence) {
    int turkishSpecCount = 0;
    for (int i = 0; i < sentence.length(); i++) {
      char c = sentence.charAt(i);
      if (TurkishAlphabet.INSTANCE.isTurkishSpecific(c)) {
        turkishSpecCount++;
      }
    }
    double ratio = turkishSpecCount * 1d / sentence.length();
    return ratio < 0.05;
  }

  private static class Hypothesis implements Scorable {

    // for a three gram model, holds the 2 history words.
    Candidate[] history;
    Candidate current;

    // required for back tracking.
    Hypothesis previous;

    float score;

    @Override
    public float getScore() {
      return score;
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

      if (!Arrays.equals(history, that.history)) {
        return false;
      }
      return current.equals(that.current);
    }

    @Override
    public int hashCode() {
      int result = Arrays.hashCode(history);
      result = 31 * result + current.hashCode();
      return result;
    }

    @Override
    public String toString() {
      return "Hypothesis{" +
          "history=" + Arrays.toString(history) +
          ", current=" + current +
          ", score=" + score +
          '}';
    }
  }

  /**
   * Represents a candidate word.
   */
  private static class Candidate {

    final String content;
    final float score;

    Candidate(String content) {
      this.content = content;
      score = 1f;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Candidate candidate = (Candidate) o;

      return content.equals(candidate.content);
    }

    @Override
    public int hashCode() {
      return content.hashCode();
    }

    @Override
    public String toString() {
      return "Candidate{" +
          "content='" + content + '\'' +
          ", score=" + score +
          '}';
    }
  }

  private static class Candidates {

    String word;
    List<Candidate> candidates;

    Candidates(String word,
        List<Candidate> candidates) {
      this.word = word;
      this.candidates = candidates;
    }

    @Override
    public String toString() {
      return "Candidates{" +
          "word='" + word + '\'' +
          ", candidates=" + candidates +
          '}';
    }
  }

  private ActiveList<Hypothesis> current = new ActiveList<>();
  private ActiveList<Hypothesis> next = new ActiveList<>();

  private static Candidate START = new Candidate("<s>");
  private static Candidate END = new Candidate("</s>");


  List<String> decode(String sentence) {

    List<Token> tokens = preProcess(sentence);

    List<Candidates> candidatesList = new ArrayList<>();

    int lmOrder = lm.getOrder();

    for (int i = 0; i < tokens.size(); i++) {
      Token currentToken = tokens.get(i);
      String current = currentToken.getText();
      String next = i == tokens.size() - 1 ? null : tokens.get(i + 1).getText();
      String previous = i == 0 ? null : tokens.get(i - 1).getText();
      List<String> spellCandidates = getSpellCandidates(currentToken, previous, next);
      if (spellCandidates.size() > 5) {
        spellCandidates = new ArrayList<>(spellCandidates.subList(0, 5));
      }
      if (spellCandidates.isEmpty()) {
        spellCandidates = Lists.newArrayList(current);
      }
      Candidates candidates = new Candidates(
          currentToken.getText(),
          spellCandidates.stream().map(Candidate::new).collect(Collectors.toList()));
      candidatesList.add(candidates);
    }

    // Path with END tokens.
    candidatesList.add(new Candidates("</s>", Collections.singletonList(END)));

    Hypothesis initial = new Hypothesis();
    initial.history = new Candidate[lmOrder - 1];
    Arrays.fill(initial.history, START);
    initial.current = START;
    initial.score = 0f;
    current.add(initial);

    for (Candidates candidates : candidatesList) {
      for (Hypothesis h : current) {
        for (Candidate c : candidates.candidates) {
          Hypothesis newHyp = new Hypothesis();
          Candidate[] hist = new Candidate[lmOrder - 1];
          if (lmOrder > 2) {
            System.arraycopy(h.history, 1, hist, 0, lmOrder - 1);
          }
          hist[hist.length - 1] = h.current;
          newHyp.current = c;
          newHyp.history = hist;
          newHyp.previous = h;

          // score calculation.
          int[] indexes = new int[lmOrder];
          for (int j = 0; j < lmOrder - 1; j++) {
            indexes[j] = lm.getVocabulary().indexOf(hist[j].content);
          }
          indexes[lmOrder - 1] = lm.getVocabulary().indexOf(c.content);
          float score = lm.getProbability(indexes);

          newHyp.score = h.score + score;
          next.add(newHyp);
        }
      }
      current = next;
      next = new ActiveList<>();
    }

    Hypothesis best = current.getBest();
    List<String> seq = new ArrayList<>();
    Hypothesis h = best;
    while (h != null && h.current != START) {
      seq.add(h.current.content);
      h = h.previous;
    }
    Collections.reverse(seq);
    return seq;
  }


  private List<Token> preProcess(String sentence) {
    List<Token> tokens = TurkishTokenizer.DEFAULT.tokenize(sentence);
    String s = combineNecessaryWords(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = splitNecessaryWords(tokens, false);
    if (probablyRequiresDeasciifier(s)) {
      Deasciifier deasciifier = new Deasciifier(s);
      s = deasciifier.convertToTurkish();
    }
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = combineNecessaryWords(tokens);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);
    s = splitNecessaryWords(tokens, true);
    tokens = TurkishTokenizer.DEFAULT.tokenize(s);

    s = useInformalAnalysis(tokens);
    return TurkishTokenizer.DEFAULT.tokenize(s);
  }


}
