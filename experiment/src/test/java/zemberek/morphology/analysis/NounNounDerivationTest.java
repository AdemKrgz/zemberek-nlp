package zemberek.morphology.analysis;

import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology.analyzer.AnalysisResult;
import zemberek.morphology.analyzer.InterpretingAnalyzer;

public class NounNounDerivationTest extends AnalyzerTestBase {

  @Test
  public void noun2Noun_1() {
    InterpretingAnalyzer analyzer = getAnalyzer("kitap");
    String in = "kitapçık";
    List<AnalysisResult> results = analyzer.analyze(in);
    printAndSort(in, results);
    Assert.assertEquals(1, results.size());
    AnalysisResult first = results.get(0);
    Assert.assertTrue(containsMorpheme(first, "Dim"));
  }

}
