package zemberek.morphology.analysis;

import org.junit.Test;
import zemberek.morphology.analyzer.InterpretingAnalyzer;

public class VerbsAfterNounAdjTest extends AnalyzerTestBase {

  @Test
  public void expectsSingleResult() {
    InterpretingAnalyzer analyzer = getAnalyzer("mavi [P:Adj]");
    expectSuccess(analyzer, 1, "maviyim");
    expectSuccess(analyzer, 1, "maviydim");
    expectSuccess(analyzer, 1, "maviyimdir");
    expectSuccess(analyzer, 1, "maviydi");
    expectSuccess(analyzer, 1, "mavidir");
    expectSuccess(analyzer, 1, "maviliyimdir");
  }

  @Test
  public void expectsSingleResult2() {
    InterpretingAnalyzer analyzer = getAnalyzer("elma");
    expectSuccess(analyzer, 1, "elmayım");
    expectSuccess(analyzer, 1, "elmaydım");
    expectSuccess(analyzer, 1, "elmayımdır");
    expectSuccess(analyzer, 1, "elmaydı");
    expectSuccess(analyzer, 1, "elmadır");
    expectSuccess(analyzer, 1, "elmayadır");
    expectSuccess(analyzer, 1, "elmayayımdır");
  }

  @Test
  public void incorrect1() {
    InterpretingAnalyzer analyzer = getAnalyzer("elma");
    expectFail(analyzer,
        "elmaydıdır",
        "elmayıdır",
        "elmamdırım",
        "elmamdımdır"
    );
  }

  @Test
  public void degilTest() {
    AnalysisTester tester = getTester("değil [P:Verb]");
    tester.checkSingleAnalysis("değil", matchesLexicalFormTail("Neg + Pres + A3sg"));
    tester.checkSingleAnalysis("değildi", matchesLexicalFormTail("Neg + Past + A3sg"));
    tester.checkSingleAnalysis("değilim", matchesLexicalFormTail("Neg + Pres + A1sg"));
  }



}
