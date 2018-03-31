package zemberek.morphology._analyzer;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import zemberek.morphology._analyzer._SingleAnalysis.MorphemeGroup;

public class _SingleAnalysisTest extends AnalyzerTestBase {

  @Test
  public void stemEndingTest() {
    InterpretingAnalyzer analyzer = getAnalyzer("kitap");
    List<_SingleAnalysis> analyses = analyzer.analyze("kitaplarda");
    Assert.assertEquals(1, analyses.size());
    _SingleAnalysis analysis = analyses.get(0);

    Assert.assertEquals(analysis.getItem(), analyzer.getLexicon().getItemById("kitap_Noun"));
    Assert.assertEquals("larda", analysis.getEnding());
    Assert.assertEquals("kitap", analysis.getStem());
  }

  @Test
  public void morphemeGroupTest() {
    InterpretingAnalyzer analyzer = getAnalyzer("kitap");

    _SingleAnalysis analysis = analyzer.analyze("kitaplarda").get(0);

    MorphemeGroup group = analysis.getGroup(0);
    Assert.assertEquals("kitaplarda", group.surface());

    analysis = analyzer.analyze("kitaplı").get(0);
    group = analysis.getGroup(0);
    Assert.assertEquals("kitap", group.surface());
    group = analysis.getGroup(1);
    Assert.assertEquals("lı", group.surface());


    analyzer = getAnalyzer("okumak");
    analysis = analyzer.analyze("okutmuyor").get(0);

    Assert.assertEquals(2, analysis.getMorphemeGroupCount());
    MorphemeGroup group0 = analysis.getGroup(0);
    Assert.assertEquals("oku", group0.surface());
    MorphemeGroup group1 = analysis.getGroup(1);
    Assert.assertEquals("tmuyor", group1.surface());
  }

  static List<String> toList(String... input) {
    return Arrays.asList(input);
  }

  @Test
  public void getStemsTest() {
    InterpretingAnalyzer analyzer = getAnalyzer("kitap");
    _SingleAnalysis analysis = analyzer.analyze("kitap").get(0);
    Assert.assertEquals(toList("kitap"), analysis.getStems());

    analysis = analyzer.analyze("kitaplı").get(0);
    Assert.assertEquals(toList("kitap","kitaplı"), analysis.getStems());

    analysis = analyzer.analyze("kitaplarda").get(0);
    Assert.assertEquals(toList("kitap"), analysis.getStems());

// TODO: check the below case.
//    analysis = analyzer.analyze("kitabımmış").get(0);
//    Assert.assertEquals(toList("kitab","kitabım"), analysis.getStems());

    analysis = analyzer.analyze("kitapçığa").get(0);
    Assert.assertEquals(toList("kitap","kitapçığ"), analysis.getStems());

    analyzer = getAnalyzer("okumak");
    analysis = analyzer.analyze("okut").get(0);
    Assert.assertEquals(toList("oku","okut"), analysis.getStems());
    analysis = analyzer.analyze("okuttur").get(0);
    Assert.assertEquals(toList("oku","okut","okuttur"), analysis.getStems());
    analysis = analyzer.analyze("okutturuluyor").get(0);
    Assert.assertEquals(toList("oku","okut","okuttur", "okutturul"), analysis.getStems());
    analysis = analyzer.analyze("okutturamıyor").get(0);
    Assert.assertEquals(toList("oku","okut","okuttur", "okuttura"), analysis.getStems());
  }

}
