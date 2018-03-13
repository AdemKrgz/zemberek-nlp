package zemberek.morphology._analyzer;

import com.google.common.base.Stopwatch;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import org.junit.Ignore;
import org.junit.Test;
import zemberek.core.logging.Log;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;

public class CoverageTest {

  @Test
  @Ignore(value = "Coverage Test.")
  public void testCoverage() throws Exception {
    String path = "../data/zemberek-oflazer/oflazer-zemberek-parsed.txt.gz";
    Log.info("Extracting coverage test file: %s", path);
    Path oflazerAndZemberek = Paths.get(path);
    InputStream gzipStream = new GZIPInputStream(new FileInputStream(oflazerAndZemberek.toFile()));
    BufferedReader reader = new BufferedReader(
        new InputStreamReader(gzipStream, StandardCharsets.UTF_8));

    ArrayDeque<String> lines = reader.lines()
        .collect(Collectors.toCollection(ArrayDeque::new));
    Log.info("File read, analyzing.");
    RootLexicon lexicon = TurkishDictionaryLoader.loadDefaultDictionaries();
    InterpretingAnalyzer analyzer = new InterpretingAnalyzer(lexicon);

    int threadCount = Runtime.getRuntime().availableProcessors();
    Log.info("Thread count = %d", threadCount);
    ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
    CompletionService<Result> service = new ExecutorCompletionService<>(executorService);

    List<String> failedWords = new ArrayList<>();

    Stopwatch sw = Stopwatch.createStarted();

    int batchCount = 0;
    int batchSize = 20_000;

    while (!lines.isEmpty()) {
      List<String> batch = new ArrayList<>(batchSize);
      int j = 0;
      while (j < batchSize && !lines.isEmpty()) {
        batch.add(lines.poll());
        j++;
      }

      if (batch.size() > 0) {
        service.submit(() -> {
          List<String> failed = new ArrayList<>(batchSize/2);
          for (String s : batch) {
            List<AnalysisResult> results = analyzer.analyze(s);
            if (results.size() == 0) {
              failed.add(s);
            }
          }
          return new Result(failed, batch.size());
        });
        batchCount++;
      }
    }

    int i = 0;
    int total = 0;
    while (i < batchCount) {
      Result r = service.take().get();
      failedWords.addAll(r.failedWords);
      total += r.wordCount;
      if (total % (batchSize * 10) == 0) {
        logResult(failedWords, total, sw);
      }
      i++;
    }

    logResult(failedWords, total, sw);
    Files.write(
        Paths.get("../data/zemberek-oflazer/new-analyzer-failed.txt"),
        failedWords, StandardCharsets.UTF_8);
  }

  class Result {

    List<String> failedWords = new ArrayList<>();
    int wordCount;

    Result(List<String> failedWords, int wordCount) {
      this.failedWords = failedWords;
      this.wordCount = wordCount;
    }
  }

  private void logResult(List<String> failedWords, int wordCount, Stopwatch sw) {
    double coverage = 100 - (failedWords.size() * 100d / wordCount);
    double seconds = sw.elapsed(TimeUnit.MILLISECONDS) / 1000d;
    double speed = wordCount / seconds;
    Log.info("Elapsed %.2f sec. %d analysed. Coverage = %.3f . Speed = %.3f tokens/sec",
        seconds, wordCount, coverage, speed);
  }


}
