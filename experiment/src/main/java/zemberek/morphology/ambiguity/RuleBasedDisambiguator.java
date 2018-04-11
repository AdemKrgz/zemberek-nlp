package zemberek.morphology.ambiguity;

import com.google.common.collect.Sets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.core.turkish.PrimaryPos;
import zemberek.morphology._analyzer._SingleAnalysis;
import zemberek.morphology._analyzer._TurkishMorphologicalAnalyzer;
import zemberek.morphology._analyzer._WordAnalysis;
import zemberek.morphology.structure.Turkish;

// Used for collecting training data.

class RuleBasedDisambiguator {

  _TurkishMorphologicalAnalyzer analyzer;
  private static Histogram<String> wordFreq;
  static List<PairRule> pairLexRules;
  static List<PairRule> endPairLexRules;
  static List<PairRule> beginPairLexRules;
  static List<PairRule> bothProperRules;

  public RuleBasedDisambiguator(_TurkishMorphologicalAnalyzer analyzer) throws IOException {
    this.analyzer = analyzer;
    Log.info("Loading 100k word frequencies.");
    List<String> freqLines = TextIO.loadLinesFromCompressedResource("/ambiguity/freq-100k.txt.gz");
    wordFreq = Histogram.loadFromLines(freqLines, ' ');
    pairLexRules = loadPairRule("/ambiguity/pair-rules.txt");
    endPairLexRules = loadPairRule("/ambiguity/end-pair-rules.txt");
    beginPairLexRules = loadPairRule("/ambiguity/begin-pair-rules.txt");
    bothProperRules = loadPairRule("/ambiguity/both-proper-rules.txt");
  }

  public ResultSentence disambiguate(String sentence) {
    List<_WordAnalysis> ambiguous = analyzer.analyzeSentence(sentence);
    ResultSentence s = new ResultSentence(sentence, ambiguous);
    s.makeDecisions();
    return s;
  }

  static class ResultSentence {

    String sentence;
    List<_WordAnalysis> sentenceAnalysis;
    List<AmbiguityAnalysis> results;

    public ResultSentence(String sentence, List<_WordAnalysis> sentenceAnalysis) {
      this.sentence = sentence;
      this.sentenceAnalysis = sentenceAnalysis;
      results = new ArrayList<>();
      for (_WordAnalysis analysis : sentenceAnalysis) {
        results.add(new AmbiguityAnalysis(analysis));
      }
    }

    int ambigiousWordCount() {
      int cnt = 0;
      for (AmbiguityAnalysis result : results) {
        if (result.choices.size() > 1) {
          long c = result.choices.stream().filter(s -> s.decision == Decision.UNDECIDED).count();
          if (c > 1) {
            cnt++;
          }
        }
      }
      return cnt;
    }

    void makeDecisions() {
      for (int i = 0; i < results.size(); i++) {
        AmbiguityAnalysis a = results.get(i);

        if (a.choices.size() == 0) {
          continue;
        }

        AmbiguityAnalysis before = i == 0 ? null : results.get(i - 1);
        AmbiguityAnalysis next = i == results.size() - 1 ? null : results.get(i + 1);

        if (a.choices.size() > 1) {
          for (int j = 0; j < a.choices.size(); j++) {
            AnalysisDecision first = a.choices.get(j);
            if (first.decision == Decision.IGNORE) {
              continue;
            }
            for (int k = j + 1; k < a.choices.size(); k++) {
              AnalysisDecision second = a.choices.size() > 1 ? a.choices.get(k) : null;
              if (second != null && second.decision == Decision.IGNORE) {
                continue;
              }
              ignoreOne(first, second, pairLexRules, a.input);
              oneProper(first, second, i == 0, a.input);
              oneProper(second, first, i == 0, a.input);
              bothProper(first, second, bothProperRules);
              if (before == null) {
                ignoreOne(first, second, beginPairLexRules, a.input);
              }
              if (next == null
                  || next.checkPos(PrimaryPos.Punctuation)
                  || next.checkPos(PrimaryPos.Question)) {
                ignoreOne(first, second, endPairLexRules, a.input);
              }
            }
          }
        }
      }
    }
  }

  public static class AmbigiousWord {

    AmbiguityAnalysis previous;
    AmbiguityAnalysis current;
    AmbiguityAnalysis next;

    public AmbigiousWord(_WordAnalysis previous, _WordAnalysis current, _WordAnalysis next) {
      this.previous = new AmbiguityAnalysis(previous);
      this.current = new AmbiguityAnalysis(current);
      this.next = new AmbiguityAnalysis(next);
    }
  }

  public static class AmbiguityAnalysis {

    String input;
    List<AnalysisDecision> choices = new ArrayList<>();

    AmbiguityAnalysis(_WordAnalysis wordAnalysis) {
      this.input = wordAnalysis.getInput();
      for (_SingleAnalysis analysis : wordAnalysis) {
        choices.add(new AnalysisDecision(input, analysis, Decision.UNDECIDED));
      }
    }

    boolean checkPos(PrimaryPos pos) {
      return choices.size() == 1 && choices.get(0).analysis.getItem().primaryPos.equals(pos);
    }

    /**
     * Returns choices as string list.
     * if there is only one analysis, returns the string form only.
     * if there are more than 1 analyses;
     * if there is only one analysisDecision that is Decision.UNDECIDED, add * at the end.
     * if there are more than 1 analysisDecision with Decision.UNDECIDED, just add string form.
     * For all ignored analysisDecision, add minus at the end.
     */
    List<String> getForTrainingOutput() {
      List<String> result = new ArrayList<>();
      result.add(input);
      if (choices.size() == 1) {
        result.add(choices.get(0).analysis.format());
        return result;
      }

      List<String> notIgnored = choices.stream().filter(s -> s.decision != Decision.IGNORE)
          .map(s -> s.analysis.format()).collect(Collectors.toList());
      if(notIgnored.size()==1) {
        result.add(notIgnored.get(0)+"*");
      } else {
        result.addAll(notIgnored);
      }
      List<String> ignored = choices.stream().filter(s -> s.decision == Decision.IGNORE)
          .map(s -> s.analysis.format()).collect(Collectors.toList());
      for (String s : ignored) {
        result.add(s+"-");
      }
      return result;
    }

  }

  enum Decision {
    IGNORE,
    UNDECIDED
  }

  public static class AnalysisDecision {

    String input;
    _SingleAnalysis analysis;

    Decision decision;

    @Override
    public String toString() {
      return "input='" + input + '\'' +
          ", analysis=" + analysis;
    }

    AnalysisDecision(String input, _SingleAnalysis analysis, Decision decision) {
      this.input = input;
      this.analysis = analysis;
      this.decision = decision;
    }
  }

  private static class PairRule {

    String input = "*";
    String okStr;
    String ignoreStr;

    public PairRule(String okStr, String ignoreStr) {
      this.okStr = okStr;
      this.ignoreStr = ignoreStr;
    }

    public PairRule(String input, String okStr, String ignoreStr) {
      this.input = input;
      this.okStr = okStr;
      this.ignoreStr = ignoreStr;
    }

    @Override
    public String toString() {
      return
          "input=" + input + '\'' +
              ", okStr='" + okStr + '\'' +
              ", ignoreStr='" + ignoreStr;
    }
  }

  static List<PairRule> loadPairRule(String resource) throws IOException {
    List<String> lines = TextIO.loadLinesFromResource(resource, "#");
    List<PairRule> rules = new ArrayList<>();
    for (String line : lines) {
      String[] tokens = line.trim().split("[ ]+");
      if (tokens.length != 3) {
        Log.warn("Unexpected token count in line %s in %s", line, resource);
        continue;
      }
      rules.add(new PairRule(tokens[0], tokens[1], tokens[2]));
    }
    return rules;
  }

  static void ignoreOne(
      AnalysisDecision a1,
      AnalysisDecision a2,
      List<PairRule> rulez,
      String input) {

    String lex1 = a1.analysis.formatLexical();
    String lex2 = a2.analysis.formatLexical();

    try {
      for (PairRule pairRule : rulez) {
        if (!checkInput(input, pairRule.input)) {
          continue;
        }
        String toIgnore = pairRule.ignoreStr;
        String toLeave = pairRule.okStr;
        if (toIgnore.equals("*")) {
          if (checkRuleStr(lex2, toLeave)) {
            a1.decision = Decision.IGNORE;
          }
          if (checkRuleStr(lex1, toLeave)) {
            a2.decision = Decision.IGNORE;
          }
        } else {
          if (checkRuleStr(lex1, toIgnore) && checkRuleStr(lex2, toLeave)) {
            a1.decision = Decision.IGNORE;
          }
          if (checkRuleStr(lex2, toIgnore) && checkRuleStr(lex1, toLeave)) {
            a2.decision = Decision.IGNORE;
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static boolean checkInput(String input, String ruleInput) {
    if (ruleInput.equals("*")) {
      return true;
    }
    if (ruleInput.endsWith("*")) {
      return input.startsWith(ruleInput.substring(0, ruleInput.length() - 1));
    }
    if (ruleInput.startsWith("*")) {
      return input.startsWith(ruleInput.substring(1, ruleInput.length()));
    }
    if (ruleInput.startsWith("!")) {
      return !input.equals(ruleInput.substring(1));
    }
    if (input.equals(ruleInput)) {
      return true;
    }
    return false;
  }

  private static boolean checkRuleStr(String input, String str) {
    if (str.equals("*")) {
      return true;
    }
    if (str.endsWith("$")) {
      return input.endsWith(str.substring(0, str.length() - 1));
    } else {
      return input.contains(str);
    }
  }

  static void bothProper(AnalysisDecision a1, AnalysisDecision a2, List<PairRule> rulez) {

    String lex1 = a1.analysis.formatLexical();
    String lex2 = a2.analysis.formatLexical();

    if (!isProperOrAbbrv(lex1) || !isProperOrAbbrv(lex2)) {
      return;
    }

    for (PairRule pairRule : rulez) {
      String ignore = pairRule.ignoreStr;
      String ok = pairRule.okStr;
      if (checkRuleStr(lex1, ignore) && checkRuleStr(lex2, ok)) {
        a1.decision = Decision.IGNORE;
      }
      if (checkRuleStr(lex2, ignore) && checkRuleStr(lex1, ok)) {
        a2.decision = Decision.IGNORE;
      }
    }
  }

  static void oneProper(
      AnalysisDecision a1,
      AnalysisDecision a2,
      boolean first,
      String input) {
    String lex1 = a1.analysis.formatLexical();
    String lex2 = a2.analysis.formatLexical();

    if (isProperOrAbbrv(lex1) && !isProperOrAbbrv(lex2)) {
      if ((!first && Character.isUpperCase(input.charAt(0))) || input.contains("'")) {
        a2.decision = Decision.IGNORE;
      }
      if (Character.isLowerCase(input.charAt(0)) && !input.contains("'")) {
        a1.decision = Decision.IGNORE;
      }
      if (containsAny(lex1, possession)) {
        a1.decision = Decision.IGNORE;
      }
      if ((first && Character.isUpperCase(input.charAt(0)) && !input.contains("'"))) {
        if (wordFreq.getCount(input) < wordFreq.getCount(input.toLowerCase(Turkish.LOCALE))) {
          a1.decision = Decision.IGNORE;
        }
      }
    }
  }

  private static boolean isProperOrAbbrv(String in) {
    return in.contains("Prop]") || in.contains("Abbrv]");
  }

  private static Set<String> possession = Sets.newHashSet("P1sg", "P2sg", "P1pl", "P2pl", "P3pl");

  private static boolean containsAny(String in, Set<String> set) {
    for (String s : set) {
      if (in.contains(s)) {
        return true;
      }
    }
    return false;
  }

}


