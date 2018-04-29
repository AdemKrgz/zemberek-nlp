CHANGE LOG
==========

## 0.12.0 

This release is the result of some major refactoring of the Morphology module.
There are many breaking changes.

Morphology module is re-written almost from scratch. Turkish morphotactics are now expressed in a simpler and more readable way
in the code. New analyzer handles pronouns better and probably it generates
more accurate results. But because this is a complete re-write, there might be new bugs and regressions.

Ambiguity resolution mechanism is changed. It now uses the old but popular Averaged Perceptron algorithm.
For now, it is trained with the data generated from some corpora using simple rules.
Therefore in this version disambiguation may not work so accurately. But it will improve in the upcoming releases quickly.
Nevertheless, new module is probably working better than previous releases.
Previous language model based algorithm is retired for now, but in future we may use a hybrid approach.

Default analysis representation is changed. Some examples:
    
    kitap ("book", Noun, Singular.)
    [kitap:Noun] kitap:Noun+A3sg

    kitabımda ("in my book"  Noun, Singular, First person possession, Locative)
    [kitap:Noun] kitab:Noun+A3sg+ım:P1sg+da:Loc

    dedim ("I told" Verb, past tense, first person singular)
    [demek:Verb] de:Verb+di:Past+m:P1sg

    diyerek ("By telling" Verb, derived to an adverb)
    [demek:Verb] di:Verb|yerek:ByDoingSo→Adv
    
We decided to omit displaying implicit `Pnon` and `Nom` suffixes from nouns to make it more readable.
This format is probably not final. We consider changing some morpheme names and refine the representation.

We now use Caffeine for caching analysis results. There are static and dynamic caches for speeding up the word analysis. 

Word generation mechanism is also re-written.

Dictionary serialization mechanism is written using protocol-buffers. Now initialization of `TurkishMorphology` class is faster.

There are Email, Url, Mention, HashTag, Emoticon, RomanNumeral, RegularAbbreviation, Abbreviation 
secondary POS information.  

We added examples module. This is like a copy of `turkis-nlp-examples` project. Users can see 
high level usage examples there.  

#### Breaking changes

Z3AbstractDisambiguator, TurkishMorphDisambiguator, Z3AbstractDisambiguator, Z3MarkovModelDisambiguator,
Z3ModelA removed from morphology modue.

TurkishSuffixes, TurkishSentenceAnalyzer, WordAnalyzer, SimpleGenerator, DynamicLexiconGraph,
DynamicSuffixProvider, SuffixData, SuffixSurfaceNode, StemNode, StemNodeGenerator,
Suffix, SuffixForm, SuffixProvider, SuffixSurfaceNodeGenerator  are removed from morphology modue.

TurkishMorphology analysis methods now return `WordAnalysis` object instead of `List<WordAnalysis>`
. `WordAnalysis` contains a `List<SingleAnalysis>` where analysis details can be reached. Methods like
`getStem()` or `getLemmas()` are moved from `WordAnalysis` to `SingleAnalysis`.

Generation is now handled by `WordGenerator` class. generation rules are changed so that if user does not
provide empty surface morphemes, system search through them anyway. Check `GenerateWords` example class.

#### Performance and memory footprint
System memory footprint is reduced. Analysis performance may be a bit slower but with cache, impact should be 
small. We will provide measurements later.   

#### Work that has not made this release

We wrote a port of Facebook's FastText library in Java. It can be used for word embeddings and 
classification tasks. However it is not yet ready for release. 

There is an experimental Named Entity Recognition module. But it is not yet ready for release.

## 0.11.1

TurkishSpellChecker now can load internal resources correctly.

## 0.11.0

#### Tokenization
We made a lot of changes in Tokenization module. Some of them are breaking changes.

Package name is changed from **zemberek.tokenizer** to **zemberek.tokenization** for consistency.

Now there is a better sentence extraction class called **TurkishSentenceExtractor**. This class
can split documents and paragraphs into sentences. It uses rules and a simple binary averaged perceptron algorithm for finding sentence boundaries.
 **LexerSentenceExtractor** is removed.

Tokenization is also improved. More token types are introduced. Including Date, Hashtag, Mention, URL, Email and Emoticon.
 However these tokens are not included as a POS type in morphological analysis yet. This will be done in upcoming versions.
 We changed the **ZemberekTokenizer** name to **TurkishTokenizer**. 
 There are some low-level breaking changes. Token TurkishWord is now Word
 and TurkishWordWithApos is WordWithApostrophe.
 
Please refer to [documentation](tokenization) for both sentence extraction and tokenization usage examples and test results.

#### Normalization
Zemberek now includes an alpha level spell-checker. This spell checker is intended for well formed documents. 
 It is not for automatic spell correction / normalization. Basically it can check if an individual word is 
  written correctly and give suggestions for a word. Suggestions are ranked against a small unigram language model for now.
  We will improve the spell checker in later versions.
  
  Plase refer to  [documentation](normalization) for usage examples.
  
#### Morphology
There are no big changes in morphology in this version. However several addition and fixes were made in dictionaries.
Including around 100 more Adverbs and several Question type fixes. Suffix morphotactics for Pronouns are still mostly broken.
 Please refer to these commits for changes: [1](https://github.com/ahmetaa/zemberek-nlp/commit/b67776054a5eec35be3f6b32c9bdb6fa83bc1d65)
  [2](https://github.com/ahmetaa/zemberek-nlp/commit/0810dedfebe6bf2af6498838af9a91fe37f059cb)
  [3](https://github.com/ahmetaa/zemberek-nlp/commit/230c5d6a32de8389438356606ca3d8b094f40553)
  [4](https://github.com/ahmetaa/zemberek-nlp/commit/231f7da2919cd9556b62ab454e19ea870b7d8fe0)
  [5](https://github.com/ahmetaa/zemberek-nlp/commit/465ce3c2b71c5f2dd57d1c6cb68bfdd9847a22fe) 

There was yet another memory leak fixed on use of HashMaps in RootLexicon. 

We have started a new morphotactics work in experiment module. But it is not yet usable.

#### Hyphenathion
 This module is removed and content is moved to **core** module for now. 
 It was not holding much weight and code requires an overhaul. Before 1.0.0 we may move it to normalization module. 

## 0.10.0

- Fixed a memory leak. Previous versions may suffer from this under heavy load. [87](https://github.com/ahmetaa/zemberek-nlp/issues/87)
- Re-introduce lang-id module. This provides a simple language identification API.
- Added normalization module. So far it only provides fast Levenshtein distance dictionary matching.
- Added an experiment module. This module will be used for experimental features.
- Dictionary fixes.
- Added city, village and district names.
- System can generate full jar containing all zemberek modules.
- Speed up Antlr based tokenizer. Now it is three times faster again. [89](https://github.com/ahmetaa/zemberek-nlp/issues/89)
- TurkishMorphology can be configured for not using cache and UnidentifiedTokenAnaysis.
- Eliminate static cache from TurkishMorphology [86](https://github.com/ahmetaa/zemberek-nlp/issues/86)
- Fix: Some inputs may cause excessive hypothesis generation during analysis [88](https://github.com/ahmetaa/zemberek-nlp/issues/88)
- Fix: Proper Nouns ending -nk or -og should not have Voicing attribute automatically.[83](https://github.com/ahmetaa/zemberek-nlp/issues/83)
- Fix: "foo \nabc" should be tokenized as "foo \n abc" [69](https://github.com/ahmetaa/zemberek-nlp/issues/83)
- There are some name changes.
  TurkishMorphology.TurkishMorphParserBuilder -> TurkishMorphology.Builder
  UnidentifiedTokenAnalyzer parse -> analyze  

## 0.9.3

- Improved morphological analysis coverage by cross checking with Oflazer-Analyzer. For this, a list of more than 7 million words are extracted from a 2 billion word corpora. Then a list of words that can be analyzed only by Oflazer-Analyzer is generated and Zemberek is fixed as much possible.  
- Breaking change: zemberek.morphology.parse package is now zemberek.morphology.analysis [67](https://github.com/ahmetaa/zemberek-nlp/issues/67)
- Breaking change: Several classes are renamed.  
   TurkishWordParserGenerator -> TurkishMorphology  
   SentenceMorphParse -> SentenceAnalysis  
   MorphParse -> WordAnalysis  
   WordParser -> WordAnalyzer [67](https://github.com/ahmetaa/zemberek-nlp/issues/67)
- Breaking change: Methods with name "parse" are renamed to "analyze". [67](https://github.com/ahmetaa/zemberek-nlp/issues/67)
- Custom Antlr dependency is removed. We now use latest stable Antlr version. We decided this because maintaining a patched version of Antlr was time consuming. We were using such a fork because original version was around 3 times slower. But current speed is good enough. We may remove Antlr dependency altogether in future releases because tokenization should be less strict and it should not do detailed classification. [68] (https://github.com/ahmetaa/zemberek-nlp/issues/68)
- Add Oflazer compatible secondary POS information for Postp.  [65](https://github.com/ahmetaa/zemberek-nlp/issues/65)
- Tokenization problem after capital letters after apostrophe. [64](https://github.com/ahmetaa/zemberek-nlp/issues/64)
- Cannot parse diyebil-, diyecek-, diyen-. [61](https://github.com/ahmetaa/zemberek-nlp/issues/61)
- Proper nouns should not have Voicing attribute automatically. [57](https://github.com/ahmetaa/zemberek-nlp/issues/57)
- Can parse reçelsi but not reçelimsi. [54](https://github.com/ahmetaa/zemberek-nlp/issues/54)
- Cannot parse maviceydi, yeşilcedir. [53](https://github.com/ahmetaa/zemberek-nlp/issues/53)
- Cannot parse "soyadları" [55](https://github.com/ahmetaa/zemberek-nlp/issues/55)
- Wrong start and stop indexes for abbreviation words on tokenization. [51](https://github.com/ahmetaa/zemberek-nlp/issues/51)
- Fixes in caching mechanism in TurkishMorphology.
- Added a dependency module. It does not perform parsing yet.

## 0.9.2

A lot of internal code changes. Added static and dynamic cache mechanisms for word parsing.

### Some Issues Fixed:
- Can parse [abdye ABDye] but not [abd'ye] [ABD'ye] #44
- Cannot parse words : [ cevaplandırmak çeşitlendirmek ] #42
- System can parse [ankaraya] but not [ankara'ya] #40
- Add ability to add a new Dictionary Item in run-time. #37
- resource test-lexicon-nouns.txt not found #36 (elifkus)
- Garip bir tokenization and stem problemi #30
- Cannot parse the word: yiyen #25 (volkanagun)

## 0.9.0

- First unstable public release.
- Removed language identification and spelling modules. They are different applications now.

