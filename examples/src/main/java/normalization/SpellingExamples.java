package normalization;

import java.io.IOException;
import zemberek.morphology.analyzer.TurkishMorphology;
import zemberek.normalization.TurkishSpellChecker;

public class SpellingExamples {

  public static void main(String[] args) throws IOException {
    TurkishMorphology morphology = TurkishMorphology.createWithDefaults();
    TurkishSpellChecker spellChecker = new TurkishSpellChecker(morphology);

    System.out.println("Check if written correctly.");
    String[] words = {"Ankara'ya", "Ankar'aya", "yapbileceksen", "yapabileceğinizden"};
    for (String word : words) {
      System.out.println(word + " -> " + spellChecker.check(word));
    }
    System.out.println();
    System.out.println("Give suggestions.");
    String[] toSuggest = {"Kraamanda", "okumuştk", "yapbileceksen", "oukyamıyorum"};
    for (String s : toSuggest) {
      System.out.println(s + " -> " + spellChecker.suggestForWord(s));
    }
  }
}
