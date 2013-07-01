package zemberek.morphology.apps;

import com.google.common.base.Charsets;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;
import zemberek.core.io.SimpleTextReader;
import zemberek.morphology.lexicon.RootLexicon;
import zemberek.morphology.lexicon.SuffixProvider;
import zemberek.morphology.lexicon.graph.DynamicLexiconGraph;
import zemberek.morphology.lexicon.tr.TurkishDictionaryLoader;
import zemberek.morphology.lexicon.tr.TurkishSuffixes;
import zemberek.morphology.parser.MorphParse;
import zemberek.morphology.parser.MorphParser;
import zemberek.morphology.parser.SimpleParser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Turkish Morphological Parser finds all possible parses for a Turkish word.
 */
public class TurkishMorphParser extends BaseParser {

    static String DEFAULT_FREQUENT_WORDS_FILE_PATH = "tr/top-20K-words.txt";
    static int DEFAULT_CACHE_SIZE = 5000;

    private static final Logger logger = Logger.getLogger(TurkishMorphParser.class.getName());
    private MorphParser parser;
    private SimpleMorphCache cache;

    public static class TurkishMorphParserBuilder {
        MorphParser _parser;
        SimpleMorphCache _cache;
        List<String> _lines = Lists.newArrayList();
        List<String> _cacheLines = Lists.newArrayList();

        public TurkishMorphParserBuilder addDefaultDictionaries() throws IOException {
            return addTextDictResources(TurkishDictionaryLoader.DEFAULT_DICTIONARY_RESOURCES.toArray(new String[TurkishDictionaryLoader.DEFAULT_DICTIONARY_RESOURCES.size()]));
        }

        public TurkishMorphParserBuilder addTextDictFiles(File... dictionaryFiles) throws IOException {
            for (File file : dictionaryFiles) {
                _lines.addAll(SimpleTextReader.trimmingUTF8Reader(file).asStringList());
            }
            return this;
        }

        public TurkishMorphParserBuilder addTextDictResources(String... resources) throws IOException {
            for (String resource : resources) {
                _lines.addAll(Resources.readLines(Resources.getResource(resource), Charsets.UTF_8));
            }
            return this;
        }

        public TurkishMorphParserBuilder addDefaultCache() {
            return addCache(DEFAULT_FREQUENT_WORDS_FILE_PATH, DEFAULT_CACHE_SIZE);
        }

        // limit = 0 for all.
        public TurkishMorphParserBuilder addCache(String fileName, int limit) {
            try {
                List<String> words = Resources.readLines(Resources.getResource(fileName), Charsets.UTF_8);
                if (limit > 0)
                    _cacheLines.addAll(words.subList(0, limit));
                else
                    _cacheLines = words;
            } catch (IOException e) {
                // We just log the error, cache is not essential.
                logger.log(Level.WARNING, "Error loading frequent words file " + fileName + " with reason: " + e.getMessage() + ". Caching will not be applied.");
            }
            return this;
        }

        public TurkishMorphParser build() throws IOException {
            Stopwatch sw = new Stopwatch().start();
            _parser = getMorphParser(_lines);
            logger.log(Level.INFO, "Parser ready: " + sw.elapsed(TimeUnit.MILLISECONDS) + "ms.");
            if (_cacheLines.size() > 0) {
                _cache = new SimpleMorphCache(_parser, _cacheLines);
                logger.log(Level.INFO, "Cache ready: " + sw.elapsed(TimeUnit.MILLISECONDS) + "ms.");
            }
            return new TurkishMorphParser(_parser, _cache);
        }
    }

    private static MorphParser getMorphParser(List<String> lines) {
        SuffixProvider suffixProvider = new TurkishSuffixes();
        RootLexicon lexicon = new TurkishDictionaryLoader(suffixProvider).load(lines);
        DynamicLexiconGraph graph = new DynamicLexiconGraph(suffixProvider);
        graph.addDictionaryItems(lexicon);
        return new SimpleParser(graph);
    }

    private TurkishMorphParser(MorphParser parser, SimpleMorphCache cache) {
        this.parser = parser;
        this.cache = cache;
    }

    public static TurkishMorphParser createWithDefaults() throws IOException {
        return new TurkishMorphParserBuilder().addDefaultDictionaries().addDefaultCache().build();
    }

    public static TurkishMorphParserBuilder newBuilder() {
        return new TurkishMorphParserBuilder();
    }

    public List<MorphParse> parse(String word) {
        if (cache != null) {
            List<MorphParse> result = cache.parse(word);
            return result != null ? result : parser.parse(word);
        }
        return parser.parse(word);
    }

}
