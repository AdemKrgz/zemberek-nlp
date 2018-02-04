package zemberek.morphology.analyzer;

import java.util.List;
import zemberek.morphology.lexicon.DictionaryItem;

public class AnalysisResult {

  // TODO: these two may be part of the [morphemes] list.
  DictionaryItem dictionaryItem;
  String root;

  List<MorphemeSurfaceForm> morphemes;


  public AnalysisResult(
      DictionaryItem dictionaryItem,
      String root,
      List<MorphemeSurfaceForm> morphemes) {
    this.dictionaryItem = dictionaryItem;
    this.root = root;
    this.morphemes = morphemes;
  }

  @Override
  public String toString() {
    return "AnalysisResult{" +
        "dictionaryItem=" + dictionaryItem +
        ", root='" + root + '\'' +
        ", morphemes=" + morphemes +
        '}';
  }
}
