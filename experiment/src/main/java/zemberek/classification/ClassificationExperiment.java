package zemberek.classification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import zemberek.apps.fasttext.EvaluateClassifier;
import zemberek.apps.fasttext.TrainClassifier;
import zemberek.core.collections.Histogram;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;
import zemberek.examples.classification.ClassificationBase;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.normalization.TurkishSentenceNormalizer;

public class ClassificationExperiment extends ClassificationBase {

  static Path root = Paths.get("/home/aaa/data/classification");
  static Path t1out = root.resolve("t3");
  static Path trainRaw = t1out.resolve("train-raw");
  static Path testRaw = t1out.resolve("test-raw");


  public static void main(String[] args) throws IOException {

    ClassificationExperiment experiment = new ClassificationExperiment();
    morphology = TurkishMorphology.builder()
        .setLexicon(RootLexicon.getDefault())
        .build();

    Path dataRoot = Paths.get("/home/aaa/zemberek-data");
    normalizer = new TurkishSentenceNormalizer(
        morphology,
        dataRoot.resolve("normalization"),
        dataRoot.resolve("lm/lm.2gram.slm"));

    experiment.generateData(200);

    List<String> trainRawLines = TextIO.loadLines(trainRaw);
    List<String> testRawLines = TextIO.loadLines(testRaw);

    Log.info("Train data:");
    experiment.dataInfo(trainRawLines);
    Log.info("Test data:");
    experiment.dataInfo(testRawLines);

/*    Path tokenizedTrain = t1out.resolve("train-tokenized");
    Path tokenizedTest = t1out.resolve("test-tokenized");

    experiment.generateSetTokenized(trainRawLines, tokenizedTrain);
    experiment.generateSetTokenized(testRawLines, tokenizedTest);

    experiment.evaluate(t1out, tokenizedTrain, tokenizedTest, "tokenized");

    Path lemmaTrain = t1out.resolve("train-lemma");
    Path lemmaTest = t1out.resolve("test-lemma");

    experiment.generateSetWithLemmas(trainRawLines, lemmaTrain);
    experiment.generateSetWithLemmas(testRawLines, lemmaTest);

    experiment.evaluate(t1out, lemmaTrain, lemmaTest, "lemma");*/

    Path splitTrain = t1out.resolve("train-split");
    Path splitTest = t1out.resolve("test-split");

    experiment.generateSetWithSplit(trainRawLines, splitTrain);
    experiment.generateSetWithSplit(testRawLines, splitTest);

    experiment.evaluate(t1out, splitTrain, splitTest, "split");

  }

  void generateData(int testSize) throws IOException {
    Path raw = root.resolve("raw3/all");
    Random r = new Random(1);
    List<String> lines = TextIO.loadLines(raw);
    Collections.shuffle(lines, r);

    List<String> test = lines.subList(0, testSize);
    List<String> train = lines.subList(testSize, lines.size() - 1);

    train = train.stream()
        .filter(s -> s.contains("__label__"))
        .map(s -> s.replaceAll("^\"", ""))
        .map(s -> normalizer.normalize(s))
        .collect(Collectors.toList());
    test = test.stream()
        .filter(s -> s.contains("__label__"))
        .map(s -> s.replaceAll("^\"", ""))
        .map(s -> normalizer.normalize(s))
        .collect(Collectors.toList());

    Files.createDirectories(t1out);
    Files.write(trainRaw, train);
    Files.write(testRaw, test);
  }

  private void evaluate(Path root, Path train, Path test, String name) {

    //Create model if it does not exist.
    Path modelPath = root.resolve(name + ".model");
    if (!modelPath.toFile().exists()) {
      new TrainClassifier().execute(
          "-i", train.toString(),
          "-o", modelPath.toString(),
          "--learningRate", "0.1",
          "--epochCount", "70",
          "--dimension", "100",
          "--wordNGrams", "2"/*,
          "--applyQuantization",
          "--cutOff", "25000"*/
      );
    }
    Log.info("Testing...");
    test(test, root.resolve(name + ".predictions"), modelPath);
    // test quantized models.
/*
    Log.info("Testing with quantized model...");
    test(test, root.resolve(name + ".predictions.q"), root.resolve(name + ".model.q"));
*/
  }

  private void test(Path testPath, Path predictionsPath, Path modelPath) {
    new EvaluateClassifier().execute(
        "-i", testPath.toString(),
        "-m", modelPath.toString(),
        "-o", predictionsPath.toString(),
        "-k", "2",
        "-th", "-2"
    );
  }

  void dataInfo(List<String> lines) {
    Log.info("Total lines = " + lines.size());
    Histogram<String> hist = new Histogram<>();
    lines.stream()
        .map(s -> s.substring(0, s.indexOf(' ')))
        .forEach(hist::add);
    Log.info("Categories :");
    for (String s : hist.getSortedList()) {
      Log.info(s + " " + hist.getCount(s));
    }
  }

}


