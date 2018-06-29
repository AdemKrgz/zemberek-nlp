package zemberek.ner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import zemberek.core.ScoredItem;
import zemberek.core.collections.FloatValueMap;
import zemberek.core.text.TextUtil;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;
import zemberek.morphology.analysis.WordAnalysisSurfaceFormatter;
import zemberek.tokenization.TurkishTokenizer;

/**
 * Multi-class averaged perceptron training.
 */
public class PerceptronNer {

  private Map<String, ClassModel> model;

  private TurkishMorphology morphology;
  private Gazetteers gazetteers;

  public PerceptronNer(Map<String, ClassModel> model, TurkishMorphology morphology) {
    this.model = model;
    this.morphology = morphology;
    this.gazetteers = new Gazetteers();
  }

  void saveModelAsText(Path modelRoot) throws IOException {
    for (String key : model.keySet()) {
      model.get(key).saveText(modelRoot);
    }
  }

  static PerceptronNer loadModel(Path modelRoot, TurkishMorphology morphology) throws IOException {
    Map<String, ClassModel> weightsMap = new HashMap<>();
    List<Path> files = Files.walk(modelRoot, 1)
        .filter(s -> s.toFile().getName().endsWith(".ner.model"))
        .collect(Collectors.toList());
    for (Path file : files) {
      ClassModel weights = ClassModel.loadFromText(file);
      weightsMap.put(weights.id, weights);
    }
    return new PerceptronNer(weightsMap, morphology);
  }

  public PerceptronNer(
      Map<String, ClassModel> model,
      TurkishMorphology morphology,
      Gazetteers gazetteers) {
    this.model = model;
    this.morphology = morphology;
    if (gazetteers == null) {
      gazetteers = new Gazetteers();
    }
    this.gazetteers = gazetteers;
  }

  public static ScoredItem<String> predictTypeAndPosition(
      Map<String, ClassModel> model,
      List<String> sparseKeys) {

    List<ScoredItem<String>> scores = new ArrayList<>();

    // find score for each class.
    for (String out : model.keySet()) {
      ClassModel o = model.get(out);
      float score = 0f;
      for (String s : sparseKeys) {
        score += o.sparseWeights.get(s);
      }
      scores.add(new ScoredItem<>(out, score));
    }

    // return max.
    return scores.stream().max((a, b) -> Float.compare(a.score, b.score)).get();
  }

  public NerDataSet test(NerDataSet set) {

    List<NerSentence> resultSentences = new ArrayList<>();

    for (NerSentence sentence : set.sentences) {
      List<NerToken> predictedTokens = new ArrayList<>();

      for (int i = 0; i < sentence.tokens.size(); i++) {

        NerToken currentToken = sentence.tokens.get(i);

        FeatureData data = new FeatureData(morphology, gazetteers, sentence, i);
        List<String> sparseInputs = data.getTextualFeatures();

        if (i > 0) {
          sparseInputs.add("PreType=" + predictedTokens.get(i - 1).tokenId);
        }
        if (i > 1) {
          sparseInputs.add("2PreType=" + predictedTokens.get(i - 2).tokenId);
        }
        if (i > 2) {
          sparseInputs.add("3PreType=" + predictedTokens.get(i - 3).tokenId);
        }

        ScoredItem<String> predicted = PerceptronNer.predictTypeAndPosition(model, sparseInputs);

        NerToken predictedToken = NerToken.fromTypePositionString(
            currentToken.index, currentToken.word, currentToken.normalized, predicted.item);
        predictedTokens.add(predictedToken);

      }
      NerSentence predictedSentence = new NerSentence(sentence.content, predictedTokens);
      resultSentences.add(predictedSentence);
    }
    return new NerDataSet(resultSentences);
  }

  public NerSentence findNamedEntities(String sentence) {

    TurkishTokenizer tokenizer = TurkishTokenizer.DEFAULT;
    List<String> words = tokenizer.tokenizeToStrings(sentence);
    List<NerToken> tokens = new ArrayList<>();
    int index = 0;
    for (String word : words) {
      NerToken token = new NerToken(
          index,
          word,
          NerDataSet.normalizeForNer(word), NerDataSet.OUT_TOKEN_TYPE, NePosition.OUTSIDE);
      tokens.add(token);
      index++;
    }

    NerSentence nerSentence = new NerSentence(sentence, tokens);

    List<NerToken> predictedTokens = new ArrayList<>();

    for (int i = 0; i < nerSentence.tokens.size(); i++) {

      NerToken currentToken = nerSentence.tokens.get(i);

      FeatureData data = new FeatureData(morphology, gazetteers, nerSentence, i);
      List<String> sparseInputs = data.getTextualFeatures();

      if (i > 0) {
        sparseInputs.add("PreType=" + predictedTokens.get(i - 1).tokenId);
      }
      if (i > 1) {
        sparseInputs.add("2PreType=" + predictedTokens.get(i - 2).tokenId);
      }
      if (i > 2) {
        sparseInputs.add("3PreType=" + predictedTokens.get(i - 3).tokenId);
      }

      ScoredItem<String> predicted = predictTypeAndPosition(model, sparseInputs);

      NerToken predictedToken = NerToken.fromTypePositionString(
          currentToken.index, currentToken.word, currentToken.normalized, predicted.item);
      predictedTokens.add(predictedToken);

    }
    return new NerSentence(nerSentence.content, predictedTokens);
  }

  //TODO: Gazetteers should allow multi word entities.
  static class Gazetteers {

    Set<String> locationWords = new HashSet<>();
    Set<String> organizationWords = new HashSet<>();
    Set<String> personWords = new HashSet<>();

    public Gazetteers(Path locationPath, Path organizationPath, Path personPath)
        throws IOException {
      locationWords.addAll(Files.readAllLines(locationPath));
      organizationWords.addAll(Files.readAllLines(organizationPath));
      personWords.addAll(Files.readAllLines(personPath));
    }

    public Gazetteers() {
    }
  }

  public static class ClassModel {

    String id;
    FloatValueMap<String> sparseWeights = new FloatValueMap<>();
    List<DenseWeights> denseWeights = new ArrayList<>();

    public ClassModel(String id) {
      this.id = id;
    }

    public ClassModel(String id,
        FloatValueMap<String> sparseWeights) {
      this.id = id;
      this.sparseWeights = sparseWeights;
    }

    void updateSparse(List<String> inputs, float value) {
      for (String input : inputs) {
        sparseWeights.incrementByAmount(input, value);
      }
    }

    ClassModel copy() {
      ClassModel weights = new ClassModel(id);
      weights.sparseWeights = sparseWeights.copy();
      List<DenseWeights> copy = new ArrayList<>();
      for (DenseWeights denseWeight : denseWeights) {
        copy.add(new DenseWeights(denseWeight.id, denseWeight.weights.clone()));
      }
      weights.denseWeights = copy;
      return weights;
    }

    public void saveText(Path outRoot) throws IOException {
      Path file = outRoot.resolve(id + ".ner.model");
      List<String> lines = sparseWeights.getKeyList().stream()
          .map(s -> String.format("%s %.3f", s, sparseWeights.get(s)))
          .collect(Collectors.toList());
      Files.write(file, lines);
    }

    public static ClassModel loadFromText(Path modelFile) throws IOException {
      String id = modelFile.toFile().getName().replace(".ner.model", "");
      List<String> lines = Files.readAllLines(modelFile);
      FloatValueMap<String> sparseWeights = new FloatValueMap<>();
      for (String line : lines) {
        if (line.trim().isEmpty()) {
          continue;
        }
        int spaceIndex = line.indexOf(" ");
        String key = line.substring(0, spaceIndex);
        float val = Float.parseFloat(line.substring(spaceIndex + 1));
        sparseWeights.set(key, val);
      }
      return new ClassModel(id, sparseWeights);
    }

  }

  public static class DenseWeights {

    String id;
    float[] weights;

    public DenseWeights(String id, float[] weights) {
      this.id = id;
      this.weights = weights;
    }
  }

  static class FeatureData {

    static WordAnalysisSurfaceFormatter formatter = new WordAnalysisSurfaceFormatter();
    TurkishMorphology morphology;
    Gazetteers gazetteers;
    String currentWord;
    String currentWordOrig;
    String nextWord;
    String nextWordOrig;
    String nextWord2;
    String nextWord2Orig;
    String previousWord;
    String previousWordOrig;
    String previousWord2;
    String previousWord2Orig;

    FeatureData(
        TurkishMorphology morphology,
        Gazetteers gazetteers,
        NerSentence sentence,
        int index) {
      this.morphology = morphology;
      this.gazetteers = gazetteers;
      List<NerToken> tokens = sentence.tokens;
      this.currentWord = tokens.get(index).normalized;
      this.currentWordOrig = tokens.get(index).word;
      if (index == tokens.size() - 1) {
        //this.nextWord = "</s>";
        //this.nextWord2 = "</s>";
        //this.nextWordOrig = "</s>";
        //this.nextWord2Orig = "</s>";
      } else if (index == tokens.size() - 2) {
        this.nextWord = tokens.get(index + 1).normalized;
        //this.nextWord2 = "</s>";
        this.nextWordOrig = tokens.get(index + 1).word;
        this.nextWord2Orig = tokens.get(index + 1).word;
      } else {
        this.nextWord = tokens.get(index + 1).normalized;
        this.nextWord2 = tokens.get(index + 2).normalized;
        this.nextWordOrig = tokens.get(index + 1).word;
        this.nextWord2Orig = tokens.get(index + 1).word;
      }
      if (index == 0) {
        //this.previousWord = "<s>";
        //this.previousWord2 = "<s>";
        //this.previousWordOrig = "<s>";
        //this.previousWord2Orig = "<s>";
      } else if (index == 1) {
        this.previousWord = tokens.get(index - 1).normalized;
        //this.previousWord2 = "<s>";
        this.previousWordOrig = tokens.get(index - 1).word;
        this.previousWord2Orig = tokens.get(index - 1).word;
      } else {
        this.previousWord = tokens.get(index - 1).normalized;
        this.previousWord2 = tokens.get(index - 2).normalized;
        this.previousWordOrig = tokens.get(index - 1).word;
        this.previousWord2Orig = tokens.get(index - 1).word;
      }
    }

    void morphologicalFeatures(String word, String featurePrefix, List<String> features) {
      if (word == null) {
        return;
      }
      WordAnalysis analyses = morphology.analyze(word);
      SingleAnalysis longest =
          analyses.analysisCount() > 0 ?
              analyses.getAnalysisResults().get(analyses.analysisCount() - 1) :
              SingleAnalysis.unknown(word);
      for (SingleAnalysis analysis : analyses) {
        if (analysis.isUnknown()) {
          return;
        }
        if (analysis == longest) {
          continue;
        }
        List<String> cLemmas = analysis.getLemmas();
        List<String> lLemmas = longest.getLemmas();

        if (cLemmas.get(cLemmas.size() - 1).length() >
            lLemmas.get(lLemmas.size() - 1).length()) {
          longest = analysis;
        }
      }
      List<String> lemmas = longest.getLemmas();
      features.add(featurePrefix + "Stem:" + longest.getStem());
      String ending = longest.getEnding();
      if (ending.length() > 0) {
        features.add(featurePrefix + "Ending:" + ending);
      }
      features.add(featurePrefix + "LongLemma:" + lemmas.get(lemmas.size() - 1));
      features.add(featurePrefix + "POS:" + longest.getPos());
      features.add(featurePrefix + "LastIg:" + longest.getLastGroup().lexicalForm());

      //features.add(featurePrefix + "ContainsProper:" + containsProper);

      if (featurePrefix.equals("CW")) {
        for (String lemma : lemmas) {
          if (gazetteers.organizationWords.contains(lemma) ||
              gazetteers.organizationWords.contains(lemma.toLowerCase())) {
            features.add(featurePrefix + "NW_Org_Gzt" + lemma);
            break;
          }
        }
        for (String lemma : lemmas) {
          if (gazetteers.personWords.contains(lemma) ||
              gazetteers.personWords.contains(lemma.toLowerCase())) {
            features.add(featurePrefix + "NW_Per_Gzt" + lemma);
            break;
          }
        }

        for (String lemma : lemmas) {
          if (gazetteers.locationWords.contains(lemma) ||
              gazetteers.locationWords.contains(lemma.toLowerCase())) {
            features.add(featurePrefix + "NW_Loc_Gzt" + lemma);
            break;
          }
        }
      }
    }

    List<String> getTextualFeatures() {
      List<String> features = new ArrayList<>();
      features.add("CW:" + currentWord);
      features.add("NW:" + nextWord);
      features.add("2NW:" + nextWord2);
      features.add("PW:" + previousWord);
      features.add("2PW:" + previousWord2);

      wordFeatures(currentWordOrig, "CW", features);
      wordFeatures(previousWordOrig, "PW", features);
      //wordFeatures(previousWord2Orig, "PW2", features);
      wordFeatures(nextWordOrig, "NW", features);
      //wordFeatures(nextWord2Orig, "NW2", features);

      morphologicalFeatures(currentWordOrig, "CW", features);
      morphologicalFeatures(previousWordOrig, "PW", features);
      //morphologicalFeatures(previousWord2Orig, "PW2", features);
      morphologicalFeatures(nextWordOrig, "NW", features);
      //morphologicalFeatures(nextWord2Orig, "NW2", features);

      String cwLast2 =
          currentWord.length() > 2 ? currentWord.substring(currentWord.length() - 2) : "";
      if (cwLast2.length() > 0) {
        features.add("CWLast2:" + cwLast2);
      }
      String cwLast3 =
          currentWord.length() > 3 ? currentWord.substring(currentWord.length() - 3) : "";
      if (cwLast3.length() > 0) {
        features.add("CWLast3:" + cwLast3);
      }

      String cwFirst2 = currentWord.length() > 2 ? currentWord.substring(0, 2) : "";
      if (cwFirst2.length() > 0) {
        features.add("CWFirst2:" + cwFirst2);
      }
      String cwFirst3 = currentWord.length() > 3 ? currentWord.substring(0, 3) : "";
      if (cwFirst3.length() > 0) {
        features.add("CWFirst3:" + cwFirst3);
      }

      return features;
    }

    void wordFeatures(String word, String featurePrefix, List<String> features) {
      if (word == null) {
        return;
      }
      features.add(featurePrefix + "Upper:" + Character.isUpperCase(word.charAt(0)));
      features.add(featurePrefix + "Punct:" + (word.length() == 1));
      boolean allCap = true;
      for (char c : word.toCharArray()) {
        if (!Character.isUpperCase(c)) {
          allCap = false;
          break;
        }
      }
      features.add(featurePrefix + "AllCap:" + allCap);
      String s = TextUtil.normalizeApostrophes(word);
      int apostropheIndex = s.indexOf('\'');
      features.add(featurePrefix + "Apost:" + (apostropheIndex >= 0));
      if (apostropheIndex >= 0) {
        String stem = word.substring(0, apostropheIndex);
        String ending = word.substring(apostropheIndex + 1);
        features.add(featurePrefix + "Stem:" + stem);
        features.add(featurePrefix + "Ending:" + ending);
      }
    }
  }

}
