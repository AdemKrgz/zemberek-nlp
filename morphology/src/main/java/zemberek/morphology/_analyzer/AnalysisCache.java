package zemberek.morphology._analyzer;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.base.Stopwatch;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import zemberek.core.logging.Log;
import zemberek.core.text.TextIO;

/**
 * A simple analysis cache. Can be shared between threads.
 */
class AnalysisCache {

  private static final int STATIC_CACHE_CAPACITY = 5000;
  private static final int DEFAULT_INITIAL_DYNAMIC_CACHE_CAPACITY = 1000;
  private static final int DEFAULT_MAX_DYNAMIC_CACHE_CAPACITY = 10000;

  private static final String MOST_USED_WORDS_FILE = "/tr/first-10K";
  private ConcurrentHashMap<String, _WordAnalysis> staticCache;
  private Semaphore staticCacheSemaphore = new Semaphore(1);
  private boolean staticCacheInitialized = false;
  private long staticCacheHits;
  private long staticCacheMiss;
  private Cache<String, _WordAnalysis> dynamicCache;

  public static AnalysisCache INSTANCE = Singleton.Instance.cache;

  private enum Singleton {
    Instance;
    AnalysisCache cache = new AnalysisCache();
  }

  // TODO(add a builder)
  private AnalysisCache() {
    dynamicCache = Caffeine.newBuilder()
        .recordStats()
        .initialCapacity(DEFAULT_INITIAL_DYNAMIC_CACHE_CAPACITY)
        .maximumSize(DEFAULT_MAX_DYNAMIC_CACHE_CAPACITY)
        .build();
    staticCache = new ConcurrentHashMap<>(STATIC_CACHE_CAPACITY);
  }

  public void initializeStaticCache(Function<String, _WordAnalysis> analysisProvider) {
    try {
      staticCacheSemaphore.acquire();
      // Some other thread already initiated priming of cache, bail out.
      if (staticCacheInitialized) {
        return;
      }
      new Thread(() -> {
        try {
          Stopwatch stopwatch = Stopwatch.createStarted();
          List<String> words = TextIO.loadLinesFromResource(MOST_USED_WORDS_FILE);
          Log.info("File read in %d ms.", stopwatch.elapsed(TimeUnit.MILLISECONDS));
          // TODO(make configurable)
          int size = Math.min(STATIC_CACHE_CAPACITY, words.size());
          for (int i = 0; i < size; i++) {
            String word = words.get(i);
            staticCache.put(word, analysisProvider.apply(word));
          }
          Log.info("Static cache initialized with %d most frequent words", size);
          Log.info("Initialization time: %d ms.", stopwatch.elapsed(TimeUnit.MILLISECONDS));
        } catch (IOException e) {
          Log.error("Could not read most frequent words list, static cache is disabled.");
          e.printStackTrace();
        }
      }).start();
      staticCacheInitialized = true;
    } catch (Exception e) {
      Log.exception(e);
    } finally {
      staticCacheSemaphore.release();
    }
  }

  public _WordAnalysis getAnalysis(String input, Function<String, _WordAnalysis> analysisProvider) {
    _WordAnalysis analysis = staticCache.get(input);
    if (analysis != null) {
      staticCacheHits++;
      return analysis;
    }
    staticCacheMiss++;
    return dynamicCache.get(input, analysisProvider);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    long total = staticCacheHits + staticCacheMiss;
    if (total > 0) {
      sb.append(String.format("Static cache(size: %d) Hit rate: %.3f%n",
          staticCache.size(), 1.0 * (staticCacheHits) / (staticCacheHits + staticCacheMiss)));
    }
    sb.append(String.format("Dynamic cache hit rate: %.3f ", dynamicCache.stats().hitRate()));
    return sb.toString();
  }
}
