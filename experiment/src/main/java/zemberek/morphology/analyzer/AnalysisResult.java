package zemberek.morphology.analyzer;

import java.util.List;
import java.util.stream.Collectors;
import zemberek.morphology.lexicon.DictionaryItem;

public class AnalysisResult {

  // TODO: these two may be part of the [morphemes] list.
  public final DictionaryItem dictionaryItem;
  public final String root;

  List<MorphemeSurfaceForm> morphemes;


  public AnalysisResult(
      DictionaryItem dictionaryItem,
      String root,
      List<MorphemeSurfaceForm> morphemes) {
    this.dictionaryItem = dictionaryItem;
    this.root = root;
    this.morphemes = morphemes;
  }

  public DictionaryItem getDictionaryItem() {
    return dictionaryItem;
  }

  public String getRoot() {
    return root;
  }

  public List<MorphemeSurfaceForm> getMorphemes() {
    return morphemes;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("[");
    sb.append(dictionaryItem.lemma).append(":").append(root).append(" + ");
    String morphemeStr =
        String.join(" + ", morphemes.stream()
            .map(MorphemeSurfaceForm::toString)
            .collect(Collectors.toList()));
    return sb.toString() + morphemeStr + "]";
  }
}
