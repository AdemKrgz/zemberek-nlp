package zemberek.morphology.morphotactics;

public class TurkishMorphotactics {

    //-------------- Morphemes ------------------------

    Morpheme noun = new Morpheme("Noun");

    // Number-Person agreement.

    // Third person singular suffix. "elma = apple"
    Morpheme a3sg = new Morpheme("A3sg");
    // Third person plural suffix. "elma-lar = apples"
    Morpheme a3pl = new Morpheme("A3pl");

    // Possessive

    // No possession suffix. This is not a real morpheme but adds information to analysis. "elma = apple"
    Morpheme pnon = new Morpheme("Pnon");
    // First person singular possession suffix.  "elma-m = my apple"
    Morpheme p1sg = new Morpheme("P1sg");
    // Third person singular possession suffix. "elma-sı = his/her apple"
    Morpheme p3sg = new Morpheme("P3sg");

    // Case suffixes

    // Nominal case suffix. It has no surface form (no letters). "elma = apple"
    Morpheme nom = new Morpheme("Nom");
    // Dative case suffix. "elmaya = to apple"
    Morpheme dat = new Morpheme("Dat");

    // Derivation suffixes

    // Diminutive suffix. Noun to Noun conversion. "elmacık = small apple, poor apple"
    Morpheme dim = new Morpheme("Dim");

    // This will be used for some states that do not have actual morphemes.
    Morpheme empty = new Morpheme("Empty");


    //-------------- States ------------------------
    // _ST = Terminal state _SnT = Non Terminal State.

    LexicalState noun_SnT = LexicalState.nonTerminal("noun_SnT", noun);

    // Number-Person agreement

    LexicalState a3sg_SnT = LexicalState.nonTerminal("a3sg_SnT", a3sg);
    LexicalState a3pl_SnT = LexicalState.nonTerminal("a3pl_SnT", a3pl);

    // Possessive

    LexicalState pnon_SnT = LexicalState.nonTerminal("pnon_SnT", pnon);
    LexicalState p1sg_SnT = LexicalState.nonTerminal("p1sg_SnT", p1sg);
    LexicalState p3sg_SnT = LexicalState.nonTerminal("p3sg_SnT", p3sg);

    // Case

    LexicalState nom_ST = LexicalState.terminal("nom_ST", nom);
    LexicalState nom_SnT = LexicalState.nonTerminal("nom_SnT", nom);
    LexicalState dat_ST = LexicalState.terminal("dat_ST", dat);

    // Derivation

    LexicalState dim_SnT = LexicalState.nonTerminal("dim_SnT", dim);

    /**
     * Turkish Nouns always have Noun-Person-Possession-Case morphemes.
     * Even there are no suffix characters.
     * elma -> Noun:elma - A3sg:ε - Pnon:ε - Nom:ε (Third person singular, No possession, Nominal Case)
     */
    public void addNounTransitions() {

        // ev-ε-?-?
        noun_SnT.newTransition(a3sg_SnT).empty().build();

        // ev-ler-?-?. Rejects inputs like "kitab-lar, burn-lar"
        noun_SnT.newTransition(a3pl_SnT)
                .surfaceTemplate("lAr")
                .addRule(Rules.rejectAny("vowel-expecting"))
                .build();

        // ev-ε-ε-?
        a3sg_SnT.newTransition(pnon_SnT).empty().build();

        // ev-ε-im oda-ε-m
        a3sg_SnT.newTransition(p1sg_SnT)
                .surfaceTemplate("+Im")
                .addRule(Rules.rejectOnly("su-root"))
                .build();

        // su-ε-yum. Only for "su"
        a3sg_SnT.newTransition(p1sg_SnT)
                .surfaceTemplate("+yum")
                .addRule(Rules.allowOnly("su-root"))
                .build();

        // ev-ε-i oda-ε-sı
        a3sg_SnT.newTransition(p3sg_SnT)
                .surfaceTemplate("+sI")
                .addRule(Rules.rejectOnly("su-root"))
                .build();

        // su-ε-yu. Only for "su"
        a3sg_SnT.newTransition(p3sg_SnT)
                .surfaceTemplate("yu")
                .addRule(Rules.allowOnly("su-root"))
                .build();

        // ev-ler-ε-?
        a3pl_SnT.newTransition(pnon_SnT).empty().build();

        // ev-ler-im-?
        a3pl_SnT.newTransition(p1sg_SnT).surfaceTemplate("Im").build();

        // ev-ler-i oda-lar-ı
        a3pl_SnT.newTransition(pnon_SnT).surfaceTemplate("I").build();

        // ev-?-ε-ε (ev, evler)
        pnon_SnT.newTransition(nom_ST)
                .empty()
                .addRule(Rules.rejectAny("vowel-expecting"))
                .build();

        // ev-ε-ε-ε-cik (evcik)
        // TODO: add morpheme rules.
        pnon_SnT.newTransition(dim_SnT)
                .surfaceTemplate(">cI~k")
                .addRule(Rules.rejectAny("vowel-expecting"))
                .build();


        // This is for blocking inputs like "kitab". Here because nominal case state is non terminal (nom_SnT)
        // analysis path will fail.
        pnon_SnT.newTransition(nom_SnT)
                .empty()
                .addRule(Rules.allowOnly("vowel-expecting"))
                .build();

        // ev-?-ε-e (eve, evlere)
        pnon_SnT.newTransition(dat_ST).surfaceTemplate("+yA").build();

        // This transition is for words like "içeri" or "dışarı". Those words implicitly contains Dative suffix.
        // But It is also possible to add explicit dative suffix to those words such as "içeri-ye".

        pnon_SnT.newTransition(dat_ST)
                .empty()
                .addRule(Rules.allowOnly("implicit-dative"))
                .build();

        // ev-?-im-ε (evim, evlerim)
        p1sg_SnT.newTransition(nom_ST).empty().build();

        // ev-?-im-e (evime, evlerime)
        p1sg_SnT.newTransition(dat_ST).surfaceTemplate("A").build();

        //ev-?-i-ε (evi, evleri)
        p3sg_SnT.newTransition(nom_SnT).empty().build();

        //ev-?-i-ε (evine, evlerine)
        p3sg_SnT.newTransition(dat_ST).surfaceTemplate("nA").build();



    }

    public TurkishMorphotactics() {
        addNounTransitions();
    }

    public static void main(String[] args) {
        TurkishMorphotactics tm = new TurkishMorphotactics();
    }

}
