package zemberek.apps.morphology;

import com.beust.jcommander.Parameter;
import java.util.Scanner;
import zemberek.apps.ConsoleApp;
import zemberek.morphology.TurkishMorphology;
import zemberek.morphology.TurkishMorphology.Builder;
import zemberek.morphology.analysis.SentenceAnalysis;
import zemberek.morphology.analysis.SentenceWordAnalysis;
import zemberek.morphology.analysis.SingleAnalysis;
import zemberek.morphology.analysis.WordAnalysis;

public class MorphologyConsole extends ConsoleApp {

  @Parameter(names = {"--disableUnknownAnalysis", "-no_unk"},
      description = "If used, unknown words will not be analysed. Such as out of dictionary "
          + "Proper nouns or numerals.")
  public boolean disableUnknownAnalysis;

  public static void main(String[] args) {
    new MorphologyConsole().execute(args);
  }

  @Override
  public String description() {
    return "Applies morphological analysis and disambiguation to user entries.";
  }

  @Override
  public void run() {
    Builder b = TurkishMorphology.builder();
    b.addDefaultBinaryDictionary();
    if (disableUnknownAnalysis) {
      b.disableUnidentifiedTokenAnalyzer();
    }
    TurkishMorphology morphology = b.build();
    String input;
    System.out.println("Enter word or sentence. Type `quit` or `Ctrl+C` to exit.:");
    Scanner sc = new Scanner(System.in);
    input = sc.nextLine();
    while (!input.equals("quit")) {

      if (input.trim().length() == 0) {
        System.out.println("Empty line cannot be processed.");
        input = sc.nextLine();
        continue;
      }

      SentenceAnalysis analysis = morphology.analyzeAndDisambiguate(input);

      System.out.format("%nS:%s%n", input);
      for (SentenceWordAnalysis sw : analysis) {
        WordAnalysis wa = sw.getWordAnalysis();
        System.out.println(wa.getInput());

        SingleAnalysis best = sw.getBestAnalysis();
        for (SingleAnalysis singleAnalysis : wa) {
          boolean isBest = singleAnalysis.equals(best);
          if (wa.analysisCount() == 1) {
            System.out.println(singleAnalysis.formatLong());
          } else {
            System.out.format("%s%s%n", singleAnalysis.formatLong(), isBest ? "*" : "");
          }
        }
      }
      System.out.println();

      input = sc.nextLine();
    }
  }
}
