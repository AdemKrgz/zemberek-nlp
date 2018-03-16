package zemberek.morphology._analyzer;

import org.junit.Test;

public class VerbsTest extends AnalyzerTestBase {

  @Test
  public void Imp() {
    AnalysisTester t = getTester("okumak");

    t.expectSingle("oku", matchesTailLex("Verb + Imp + A2sg"));
    t.expectSingle("okusun", matchesTailLex("Verb + Imp + A3sg"));
    t.expectSingle("okuyun", matchesTailLex("Verb + Imp + A2pl"));
    t.expectSingle("okuyunuz", matchesTailLex("Verb + Imp + A2pl"));
    t.expectSingle("okusunlar", matchesTailLex("Verb + Imp + A3pl"));

    t = getTester("yazmak");

    t.expectSingle("yaz", matchesTailLex("Verb + Imp + A2sg"));
    t.expectSingle("yazsın", matchesTailLex("Verb + Imp + A3sg"));
    t.expectSingle("yazın", matchesTailLex("Verb + Imp + A2pl"));
    t.expectSingle("yazınız", matchesTailLex("Verb + Imp + A2pl"));
    t.expectSingle("yazsınlar", matchesTailLex("Verb + Imp + A3pl"));
  }

  @Test
  public void ImpNeg() {
    AnalysisTester t = getTester("okumak");

    t.expectAny("okuma", matchesTailLex("Verb + Neg + Imp + A2sg"));
    t.expectAny("okumasın", matchesTailLex("Verb + Neg + Imp + A3sg"));
    t.expectSingle("okumayın", matchesTailLex("Verb + Neg + Imp + A2pl"));
    t.expectSingle("okumayınız", matchesTailLex("Verb + Neg + Imp + A2pl"));
    t.expectSingle("okumasınlar", matchesTailLex("Verb + Neg + Imp + A3pl"));
  }


  @Test
  public void progressivePositive() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazıyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    t.expectSingle("yazıyorsun", matchesTailLex("Verb + Prog1 + A2sg"));
    t.expectSingle("yazıyor", matchesTailLex("Verb + Prog1 + A3sg"));
    t.expectSingle("yazıyoruz", matchesTailLex("Verb + Prog1 + A1pl"));
    t.expectSingle("yazıyorsunuz", matchesTailLex("Verb + Prog1 + A2pl"));
    t.expectSingle("yazıyorlar", matchesTailLex("Verb + Prog1 + A3pl"));

    t = getTester("gitmek [A:Voicing]");
    t.expectSingle("gidiyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    t.expectSingle("gidiyorsun", matchesTailLex("Verb + Prog1 + A2sg"));
    t.expectSingle("gidiyor", matchesTailLex("Verb + Prog1 + A3sg"));

    t.expectFail(
        "gitiyor",
        "gidyor"
    );
  }

  @Test
  public void progressivePositive2() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazıyordum", matchesTailLex("Verb + Prog1 + Past + A1sg"));
    t.expectSingle("yazıyorsan", matchesTailLex("Verb + Prog1 + Cond + A2sg"));
    t.expectSingle("yazıyormuş", matchesTailLex("Verb + Prog1 + Narr + A3sg"));
    t.expectSingle("yazıyorduk", matchesTailLex("Verb + Prog1 + Past + A1pl"));

    t.expectFail(
        "yazıyormuşsak"
    );
  }

  @Test
  public void progressiveDrop() {
    AnalysisTester t = getTester("aramak");

    t.expectSingle("arıyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    t.expectSingle("arıyor", matchesTailLex("Verb + Prog1 + A3sg"));

    t.expectFail(
        "arayorum"
    );

    t = getTester("yürümek");

    t.expectSingle("yürüyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    t.expectSingle("yürüyor", matchesTailLex("Verb + Prog1 + A3sg"));

    t = getTester("denemek");

    t.expectSingle("deniyorum", matchesTailLex("Verb + Prog1 + A1sg"));
    t.expectSingle("deniyor", matchesTailLex("Verb + Prog1 + A3sg"));
  }

  @Test
  public void progressiveNegative() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazmıyorum", matchesTailLex("Verb + Neg + Prog1 + A1sg"));
    t.expectSingle("yazmıyorsun", matchesTailLex("Verb + Neg + Prog1 + A2sg"));
    t.expectSingle("yazmıyor", matchesTailLex("Verb + Neg + Prog1 + A3sg"));

    t.expectFail(
        "yazmayorum"
    );

    t = getTester("aramak");

    t.expectSingle("aramıyoruz", matchesTailLex("Verb + Neg + Prog1 + A1pl"));
    t.expectSingle("aramıyorsunuz", matchesTailLex("Verb + Neg + Prog1 + A2pl"));
    t.expectSingle("aramıyorlar", matchesTailLex("Verb + Neg + Prog1 + A3pl"));

    t.expectFail(
        "aramayoruz",
        "armıyoruz",
        "armıyor"
    );

    t = getTester("affetmek [A:Voicing]");
    t.expectSingle("affetmiyor", matchesTailLex("Verb + Neg + Prog1 + A3sg"));

  }

  @Test
  public void progressiveNegative2() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazmıyordum", matchesTailLex("Verb + Neg + Prog1 + Past + A1sg"));
    t.expectSingle("yazmıyorsan", matchesTailLex("Verb + Neg + Prog1 + Cond + A2sg"));
    t.expectSingle("yazmıyormuş", matchesTailLex("Verb + Neg + Prog1 + Narr + A3sg"));
    t.expectSingle("yazmıyorduk", matchesTailLex("Verb + Neg + Prog1 + Past + A1pl"));

    t = getTester("aramak");

    t.expectSingle("aramıyorduk", matchesTailLex("Verb + Neg + Prog1 + Past + A1pl"));
    t.expectSingle("aramıyorsam", matchesTailLex("Verb + Neg + Prog1 + Cond + A1sg"));
    t.expectSingle("aramıyormuşuz", matchesTailLex("Verb + Neg + Prog1 + Narr + A1pl"));
  }

  @Test
  public void aorist() {
    AnalysisTester t = getTester("yazmak"); // Aorist_A attribute is inferred.

    t.expectSingle("yazarım", matchesTailLex("Verb + Aor + A1sg"));
    t.expectSingle("yazarsın", matchesTailLex("Verb + Aor + A2sg"));
    t.expectSingle("yazar", matchesTailLex("Verb + Aor + A3sg"));
    t.expectSingle("yazarız", matchesTailLex("Verb + Aor + A1pl"));
    t.expectSingle("yazarlar", matchesTailLex("Verb + Aor + A3pl"));

    t.expectSingle("yazdırır",
        matchesTailLex("Verb + Caus + Verb + Aor + A3sg"));
    t.expectSingle("yazdırtır",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Aor + A3sg"));
    t.expectSingle("yazdırttırır",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Caus + Verb + Aor + A3sg"));

    t = getTester("semirmek");
    t.expectSingle("semiririm", matchesTailLex("Verb + Aor + A1sg"));
    t.expectSingle("semirirsin", matchesTailLex("Verb + Aor + A2sg"));
    t.expectSingle("semirir", matchesTailLex("Verb + Aor + A3sg"));

    t.expectSingle("semirtirim",
        matchesTailLex("Verb + Caus + Verb + Aor + A1sg"));
    t.expectSingle("semirttiririm",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Aor + A1sg"));
  }

  @Test
  public void aorist2() {
    AnalysisTester t = getTester("yazmak"); // Aorist_A attribute is inferred.

    t.expectSingle("yazardım", matchesTailLex("Verb + Aor + Past + A1sg"));
    t.expectSingle("yazmazdım", matchesTailLex("Verb + Neg + Aor + Past + A1sg"));
    t.expectSingle("yazardık", matchesTailLex("Verb + Aor + Past + A1pl"));
    t.expectSingle("yazarmışsın", matchesTailLex("Verb + Aor + Narr + A2sg"));
    t.expectSingle("yazarsa", matchesTailLex("Verb + Aor + Cond + A3sg"));
    t.expectSingle("yazmazsa", matchesTailLex("Verb + Neg + Aor + Cond + A3sg"));
    t.expectSingle("yazmazmışız", matchesTailLex("Verb + Neg + Aor + Narr + A1pl"));

    t = getTester("etmek [A:Voicing]");
    t.expectSingle("eder", matchesTailLex("Verb + Aor + A3sg"));
    t.expectSingle("edermiş", matchesTailLex("Verb + Aor + Narr + A3sg"));
    t.expectSingle("etmezmiş", matchesTailLex("Verb + Neg + Aor + Narr + A3sg"));
    t.expectSingle("ederdik", matchesTailLex("Verb + Aor + Past + A1pl"));
    t.expectSingle("etmezsek", matchesTailLex("Verb + Neg + Aor + Cond + A1pl"));
  }

  @Test
  public void aoristNegative() {
    AnalysisTester t = getTester("yazmak"); // Aorist_A attribute is inferred.

    t.expectAny("yazmam", matchesTailLex("Verb + Neg + Aor + A1sg"));
    t.expectAny("yazmam", matchesTailLex("Verb + Inf2 + Noun + A3sg + P1sg + Nom"));
    t.expectSingle("yazmazsın", matchesTailLex("Verb + Neg + Aor + A2sg"));
    t.expectSingle("yazmaz", matchesTailLex("Verb + Neg + Aor + A3sg"));
    t.expectAny("yazmayız", matchesTailLex("Verb + Neg + Aor + A1pl"));
    t.expectSingle("yazmazsınız", matchesTailLex("Verb + Neg + Aor + A2pl"));
    t.expectSingle("yazmazlar", matchesTailLex("Verb + Neg + Aor + A3pl"));

    t.expectSingle("yazdırmaz",
        matchesTailLex("Verb + Caus + Verb + Neg + Aor + A3sg"));
    t.expectSingle("yazdırtmaz",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Neg + Aor + A3sg"));
    t.expectSingle("yazdırttırmazsınız",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Caus + Verb + Neg + Aor + A2pl"));

    t = getTester("semirmek");
    t.expectAny("semirmem", matchesTailLex("Verb + Neg + Aor + A1sg"));
    t.expectSingle("semirmezsin", matchesTailLex("Verb + Neg + Aor + A2sg"));
    t.expectSingle("semirmez", matchesTailLex("Verb + Neg + Aor + A3sg"));

    t.expectAny("semirtmem",
        matchesTailLex("Verb + Caus + Verb + Neg + Aor + A1sg"));
    t.expectAny("semirttirmem",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Neg + Aor + A1sg"));
  }

  @Test
  public void abilityPositive() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazabil", matchesTailLex("Verb + Able + Verb + Imp + A2sg"));
    t.expectSingle("yazabiliyor", matchesTailLex("Verb + Able + Verb + Prog1 + A3sg"));

    t.expectFail(
        "yazabildir",
        "yazabilebil"
    );

    t = getTester("okumak");

    t.expectSingle("okuyabil", matchesTailLex("Verb + Able + Verb + Imp + A2sg"));
    t.expectSingle("okuyabilir", matchesTailLex("Verb + Able + Verb + Aor + A3sg"));
  }

  @Test
  public void abilityAfterCausative() {

    AnalysisTester t = getTester("okumak");

    t.expectSingle("okutabil",
        matchesTailLex("Verb + Caus + Verb + Able + Verb + Imp + A2sg"));
    t.expectSingle("okutturabil",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Able + Verb + Imp + A2sg"));
  }

  @Test
  public void abilityNegative() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazama", matchesTailLex("Verb + Able + Verb + Neg + Imp + A2sg"));
    t.expectSingle("yazamaz", matchesTailLex("Verb + Able + Verb + Neg + Aor + A3sg"));
    t.expectSingle("yazamıyor", matchesTailLex("Verb + Able + Verb + Neg + Prog1 + A3sg"));

    t = getTester("okumak");

    t.expectSingle("okuyama", matchesTailLex("Verb + Able + Verb + Neg + Imp + A2sg"));
    t.expectSingle("okutmayabilir",
        matchesTailLex("Verb + Caus + Verb + Neg + Able + Verb + Aor + A3sg"));
    t.expectSingle("okutamayabilir",
        matchesTailLex("Verb + Caus + Verb + Able + Verb + Neg + Able + Verb + Aor + A3sg"));
    t.expectSingle("okuyamayabilir",
        matchesTailLex("Verb + Able + Verb + Neg + Able + Verb + Aor + A3sg"));
  }

  @Test
  public void passive1() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazıl", matchesTailLex("Verb + Pass + Verb + Imp + A2sg"));
    t.expectSingle("yazılıyor", matchesTailLex("Verb + Pass + Verb + Prog1 + A3sg"));
    t.expectSingle("yazdırılıyor",
        matchesTailLex("Verb + Caus + Verb + Pass + Verb + Prog1 + A3sg"));
    t.expectSingle("yazdırtılıyor",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Pass + Verb + Prog1 + A3sg"));

    t.expectFail(
        "yazınıyor",
        "yazınıl",
        "yazılınıl",
        "yazıldır",
        "yazdırınıyor",
        "yazdırıldır"
    );

    t = getTester("okumak");

    t.expectSingle("okun", matchesTailLex("Verb + Pass + Verb + Imp + A2sg"));
    t.expectSingle("okunul", matchesTailLex("Verb + Pass + Verb + Imp + A2sg"));
    t.expectSingle("okunulabilir",
        matchesTailLex("Verb + Pass + Verb + Able + Verb + Aor + A3sg"));
  }

  @Test
  public void passive2() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazılmasın", matchesTailLex("Verb + Pass + Verb + Neg + Imp + A3sg"));
    t.expectSingle("yazılmıyor", matchesTailLex("Verb + Pass + Verb + Neg + Prog1 + A3sg"));
    t.expectSingle("yazdırılmıyor",
        matchesTailLex("Verb + Caus + Verb + Pass + Verb + Neg + Prog1 + A3sg"));
    t.expectSingle("yazdırtılmıyor",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Pass + Verb + Neg + Prog1 + A3sg"));

    t.expectFail(
        "yazınmıyor",
        "yazınılma",
        "yazılınılma",
        "yazıldırma",
        "yazdırınmıyor",
        "yazdırıldırma"
    );

    t = getTester("okumak");

    t.expectSingle("okunmayın", matchesTailLex("Verb + Pass + Verb + Neg + Imp + A2pl"));
    t.expectSingle("okunulmasın", matchesTailLex("Verb + Pass + Verb + Neg + Imp + A3sg"));
    t.expectSingle("okunulmayabilir",
        matchesTailLex("Verb + Pass + Verb + Neg + Able + Verb + Aor + A3sg"));
    t.expectSingle("okunulamayabilir",
        matchesTailLex("Verb + Pass + Verb + Able + Verb + Neg + Able + Verb + Aor + A3sg"));
  }

  @Test
  public void past() {
    AnalysisTester t = getTester("yazmak"); 

    t.expectSingle("yazdım", matchesTailLex("Verb + Past + A1sg"));
    t.expectSingle("yazmadım", matchesTailLex("Verb + Neg + Past + A1sg"));
    t.expectAny("yazdık", matchesTailLex("Verb + Past + A1pl"));
    t.expectAny("yazmadık", matchesTailLex("Verb + Neg + Past + A1pl"));
    t.expectSingle("yazdıysan", matchesTailLex("Verb + Past + Cond + A2sg"));
    t.expectSingle("yazmadıysan", matchesTailLex("Verb + Neg + Past + Cond + A2sg"));
    t.expectSingle("yazdır", matchesTailLex("Verb + Caus + Verb + Imp + A2sg"));


    t = getTester("etmek [A:Voicing]");
    t.expectSingle("etti", matchesTailLex("Verb + Past + A3sg"));
    t.expectSingle("etmedi", matchesTailLex("Verb + Neg + Past + A3sg"));
    t.expectAny("ettik", matchesTailLex("Verb + Past + A1pl"));
    t.expectAny("etmedik", matchesTailLex("Verb + Neg + Past + A1pl"));
  }

  @Test
  public void narrative() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazmışım", matchesTailLex("Verb + Narr + A1sg"));
    t.expectSingle("yazmamışım", matchesTailLex("Verb + Neg + Narr + A1sg"));
    t.expectSingle("yazmışız", matchesTailLex("Verb + Narr + A1pl"));
    t.expectSingle("yazmamışız", matchesTailLex("Verb + Neg + Narr + A1pl"));
    t.expectSingle("yazmışsan", matchesTailLex("Verb + Narr + Cond + A2sg"));
    t.expectSingle("yazmamışsan", matchesTailLex("Verb + Neg + Narr + Cond + A2sg"));


    t = getTester("etmek [A:Voicing]");
    t.expectSingle("etmiş", matchesTailLex("Verb + Narr + A3sg"));
    t.expectSingle("etmemiş", matchesTailLex("Verb + Neg + Narr + A3sg"));
    t.expectSingle("etmişiz", matchesTailLex("Verb + Narr + A1pl"));
    t.expectSingle("etmemişiz", matchesTailLex("Verb + Neg + Narr + A1pl"));
  }

  @Test
  public void future() {
    AnalysisTester t = getTester("yazmak");

    t.expectAny("yazacağım", matchesTailLex("Verb + Fut + A1sg"));
    t.expectAny("yazmayacağım", matchesTailLex("Verb + Neg + Fut + A1sg"));
    t.expectAny("yazacağız", matchesTailLex("Verb + Fut + A1pl"));
    t.expectAny("yazmayacağız", matchesTailLex("Verb + Neg + Fut + A1pl"));
    t.expectAny("yazacaksan", matchesTailLex("Verb + Fut + Cond + A2sg"));
    t.expectAny("yazmayacaksan", matchesTailLex("Verb + Neg + Fut + Cond + A2sg"));
    t.expectAny("yazmayacaktın", matchesTailLex("Verb + Neg + Fut + Past + A2sg"));

    t.expectFail(
        "yazmayacağ",
        "yazmayacakım",
        "yazacakım",
        "yazacağsın"
    );

    t = getTester("etmek [A:Voicing]");
    t.expectAny("edecek", matchesTailLex("Verb + Fut + A3sg"));
    t.expectAny("etmeyecek", matchesTailLex("Verb + Neg + Fut + A3sg"));
    t.expectSingle("edeceğiz", matchesTailLex("Verb + Fut + A1pl"));
    t.expectSingle("etmeyeceğiz", matchesTailLex("Verb + Neg + Fut + A1pl"));
  }

  @Test
  public void future2() {
    AnalysisTester t = getTester("aramak");

    t.expectAny("arayacağım", matchesTailLex("Verb + Fut + A1sg"));
    t.expectSingle("aratacağız", matchesTailLex("Verb + Caus + Verb + Fut + A1pl"));
    t.expectSingle("arayabileceğiz", matchesTailLex("Verb + Able + Verb + Fut + A1pl"));
    t.expectSingle("aratabileceğiz",
        matchesTailLex("Verb + Caus + Verb + Able + Verb + Fut + A1pl"));
    t.expectSingle("aratmayabileceğiz",
        matchesTailLex("Verb + Caus + Verb + Neg + Able + Verb + Fut + A1pl"));
    t.expectSingle("arattıramayabileceğiz",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Able + Verb + Neg + Able + Verb + Fut + A1pl"));
  }

  @Test
  public void progressiveMakta() {
    AnalysisTester t = getTester("yazmak");

    t.expectAny("yazmakta", matchesTailLex("Verb + Prog2 + A3sg"));
    t.expectAny("yazmaktayım", matchesTailLex("Verb + Prog2 + A1sg"));
    t.expectAny("yazmamaktayım", matchesTailLex("Verb + Neg + Prog2 + A1sg"));
    t.expectAny("yazmaktayız", matchesTailLex("Verb + Prog2 + A1pl"));
    t.expectAny("yazmamaktayız", matchesTailLex("Verb + Neg + Prog2 + A1pl"));
    t.expectSingle("yazmaktaysan", matchesTailLex("Verb + Prog2 + Cond + A2sg"));
    t.expectSingle("yazmaktaymışsınız", matchesTailLex("Verb + Prog2 + Narr + A2pl"));
    // awkward but ok.
    t.expectAny("yazmamaktaydım", matchesTailLex("Verb + Neg + Prog2 + Past + A1sg"));

    t = getTester("etmek [A:Voicing]");
    t.expectAny("etmekte", matchesTailLex("Verb + Prog2 + A3sg"));
    t.expectAny("etmemekte", matchesTailLex("Verb + Neg + Prog2 + A3sg"));
    t.expectAny("etmekteyiz", matchesTailLex("Verb + Prog2 + A1pl"));
  }

  @Test
  public void progressiveMakta2() {
    AnalysisTester t = getTester("aramak");

    t.expectAny("aramaktayım", matchesTailLex("Verb + Prog2 + A1sg"));
    t.expectAny("aratmaktayız", matchesTailLex("Verb + Caus + Verb + Prog2 + A1pl"));
    t.expectAny("arayabilmekteyiz", matchesTailLex("Verb + Able + Verb + Prog2 + A1pl"));
    t.expectAny("aratabilmekteyiz",
        matchesTailLex("Verb + Caus + Verb + Able + Verb + Prog2 + A1pl"));
    t.expectAny("aratmayabilmekteyiz",
        matchesTailLex("Verb + Caus + Verb + Neg + Able + Verb + Prog2 + A1pl"));
    t.expectAny("arattıramayabilmekteyiz",
        matchesTailLex("Verb + Caus + Verb + Caus + Verb + Able + Verb + Neg + Able + Verb + Prog2 + A1pl"));
  }


  @Test
  public void demekYemek() {
    AnalysisTester t = getTester("demek [A:Special]");

    t.expectSingle("de", matchesTailLex("Verb + Imp + A2sg"));
    t.expectAny("deme", matchesTailLex("Verb + Neg + Imp + A2sg"));
    t.expectSingle("dedi", matchesTailLex("Verb + Past + A3sg"));
    t.expectSingle("demiş", matchesTailLex("Verb + Narr + A3sg"));
    t.expectSingle("den", matchesTailLex("Verb + Pass + Verb + Imp + A2sg"));
    t.expectSingle("denil", matchesTailLex("Verb + Pass + Verb + Imp + A2sg"));
    t.expectAny("diyecek", matchesTailLex("Verb + Fut + A3sg"));
    t.expectAny("diyecek", matchesTailLex("Verb + FutPart + Adj + Pnon"));
    t.expectSingle("diyebilir", matchesTailLex("Verb + Able + Verb + Aor + A3sg"));
    t.expectAny("deme", matchesTailLex("Verb + Neg + Imp + A2sg"));
    t.expectSingle("diyor", matchesTailLex("Verb + Prog1 + A3sg"));
    t.expectSingle("demiyor", matchesTailLex("Verb + Neg + Prog1 + A3sg"));
    t.expectSingle("der", matchesTailLex("Verb + Aor + A3sg"));
    t.expectSingle("demez", matchesTailLex("Verb + Neg + Aor + A3sg"));
    t.expectSingle("dedir", matchesTailLex("Verb + Caus + Verb + Imp + A2sg"));
    t.expectAny("dedirme", matchesTailLex("Verb + Caus + Verb + Neg + Imp + A2sg"));
    t.expectSingle("diye", matchesTailLex("Verb + Opt + A3sg"));
    t.expectAny("demeye", matchesTailLex("Verb + Neg + Opt + A3sg"));
    t.expectSingle("dese", matchesTailLex("Verb + Desr + A3sg"));
    t.expectSingle("demese", matchesTailLex("Verb + Neg + Desr + A3sg"));

    t.expectFail(
        "dir",
        "dimez",
        "di",
        "din",
        "didir",
        "deyor",
        "deyecek",
        "didi",
        "yeyiş",
        "diyiş",
        "dimek",
        "dime",
        "yime",
        "yimek",
        "dimiş",
        "dimiyor"
    );
  }


  @Test
  public void optative() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazayım", matchesTailLex("Verb + Opt + A1sg"));
    t.expectAny("yazmayayım", matchesTailLex("Verb + Neg + Opt + A1sg"));
    t.expectSingle("yazasın", matchesTailLex("Verb + Opt + A2sg"));
    t.expectSingle("yazmayasın", matchesTailLex("Verb + Neg + Opt + A2sg"));
    t.expectSingle("yaza", matchesTailLex("Verb + Opt + A3sg"));
    t.expectAny("yazmaya", matchesTailLex("Verb + Neg + Opt + A3sg"));
    t.expectSingle("yazalım", matchesTailLex("Verb + Opt + A1pl"));
    t.expectSingle("yazasınız", matchesTailLex("Verb + Opt + A2pl"));
    t.expectSingle("yazalar", matchesTailLex("Verb + Opt + A3pl"));
    t.expectSingle("yazaydı", matchesTailLex("Verb + Opt + Past + A3sg"));
    t.expectSingle("yazaymış", matchesTailLex("Verb + Opt + Narr + A3sg"));
    t.expectSingle("yazaymışlar", matchesTailLex("Verb + Opt + Narr + A3pl"));

    t = getTester("etmek [A:Voicing]");
    t.expectSingle("edeyim", matchesTailLex("Verb + Opt + A1sg"));
    t.expectSingle("ede", matchesTailLex("Verb + Opt + A3sg"));
    t.expectAny("etmeye", matchesTailLex("Verb + Neg + Opt + A3sg"));
  }

  @Test
  public void desire() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazsam", matchesTailLex("Verb + Desr + A1sg"));
    t.expectSingle("yazmasam", matchesTailLex("Verb + Neg + Desr + A1sg"));
    t.expectSingle("yazsan", matchesTailLex("Verb + Desr + A2sg"));
    t.expectSingle("yazmasan", matchesTailLex("Verb + Neg + Desr + A2sg"));
    t.expectSingle("yazsa", matchesTailLex("Verb + Desr + A3sg"));
    t.expectAny("yazmasa", matchesTailLex("Verb + Neg + Desr + A3sg"));
    t.expectSingle("yazsak", matchesTailLex("Verb + Desr + A1pl"));
    t.expectSingle("yazsanız", matchesTailLex("Verb + Desr + A2pl"));
    t.expectSingle("yazsalar", matchesTailLex("Verb + Desr + A3pl"));
    t.expectSingle("yazsaydı", matchesTailLex("Verb + Desr + Past + A3sg"));
    t.expectSingle("yazsaymışlar", matchesTailLex("Verb + Desr + Narr + A3pl"));

    t = getTester("etmek [A:Voicing]");
    t.expectSingle("etsem", matchesTailLex("Verb + Desr + A1sg"));
    t.expectSingle("etse", matchesTailLex("Verb + Desr + A3sg"));
    t.expectSingle("etmese", matchesTailLex("Verb + Neg + Desr + A3sg"));
  }

  @Test
  public void necessity() {
    AnalysisTester t = getTester("yazmak");

    t.expectAny("yazmalıyım", matchesTailLex("Verb + Neces + A1sg"));
    t.expectAny("yazmamalıyım", matchesTailLex("Verb + Neg + Neces + A1sg"));
    t.expectSingle("yazmalısın", matchesTailLex("Verb + Neces + A2sg"));
    t.expectSingle("yazmamalısın", matchesTailLex("Verb + Neg + Neces + A2sg"));
    t.expectAny("yazmalı", matchesTailLex("Verb + Neces + A3sg"));
    t.expectAny("yazmamalı", matchesTailLex("Verb + Neg + Neces + A3sg"));
    t.expectAny("yazmalıyız", matchesTailLex("Verb + Neces + A1pl"));
    t.expectSingle("yazmalısınız", matchesTailLex("Verb + Neces + A2pl"));
    t.expectAny("yazmalılar", matchesTailLex("Verb + Neces + A3pl"));
    t.expectAny("yazmalıydı", matchesTailLex("Verb + Neces + Past + A3sg"));
    t.expectAny("yazmalıymışlar", matchesTailLex("Verb + Neces + Narr + A3pl"));

    t = getTester("etmek [A:Voicing]");
    t.expectAny("etmeliyim", matchesTailLex("Verb + Neces + A1sg"));
    t.expectAny("etmeli", matchesTailLex("Verb + Neces + A3sg"));
    t.expectAny("etmemeli", matchesTailLex("Verb + Neg + Neces + A3sg"));
  }

  @Test
  public void a3plExceptionTest() {
    AnalysisTester t = getTester("yazmak");

    t.expectSingle("yazarlardı", matchesTailLex("Verb + Aor + A3pl + Past"));
    t.expectSingle("yazardılar", matchesTailLex("Verb + Aor + Past + A3pl"));
    t.expectSingle("yazarlarmış", matchesTailLex("Verb + Aor + A3pl + Narr"));
    t.expectSingle("yazarmışlar", matchesTailLex("Verb + Aor + Narr + A3pl"));
    t.expectSingle("yazarsalar", matchesTailLex("Verb + Aor + Cond + A3pl"));
    t.expectSingle("yazarlarsa", matchesTailLex("Verb + Aor + A3pl + Cond"));

    t.expectFail(
        "yazarlardılar",
        "yazardılardı",
        "yazarlarmışlar",
        "yazarmışlarmış",
        "yazarsalarsa",
        "yazarlarsalar",
        "yazardılarsa",
        "yazarsalarmış",
        "yazarsalardı",
        "yazarmışlarsa"
    );
  }

  @Test
  public void copula() {
    AnalysisTester t = getTester("yazmak");

    t.expectAny("yazardır", matchesTailLex("Verb + Aor + A3sg + Cop"));
    t.expectAny("yazmazdır", matchesTailLex("Verb + Neg + Aor + A3sg + Cop"));
    t.expectAny("yazacaktır", matchesTailLex("Verb + Fut + A3sg + Cop"));
    t.expectAny("yazacağımdır", matchesTailLex("Verb + Fut + A1sg + Cop"));
    t.expectAny("yazacaksındır", matchesTailLex("Verb + Fut + A2sg + Cop"));
    t.expectAny("yazacağızdır", matchesTailLex("Verb + Fut + A1pl + Cop"));
    t.expectAny("yazacaksınızdır", matchesTailLex("Verb + Fut + A2pl + Cop"));
    t.expectAny("yazacaklardır", matchesTailLex("Verb + Fut + A3pl + Cop"));
    t.expectSingle("yazıyordur", matchesTailLex("Verb + Prog1 + A3sg + Cop"));
    t.expectAny("yazmaktadır", matchesTailLex("Verb + Prog2 + A3sg + Cop"));
    t.expectAny("yazmalıdır", matchesTailLex("Verb + Neces + A3sg + Cop"));

    t.expectFail(
        "yazsadır",
        "yazdıdır"
    );
  }

}
