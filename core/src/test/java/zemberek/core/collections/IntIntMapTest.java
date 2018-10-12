package zemberek.core.collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.common.base.Stopwatch;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class IntIntMapTest {

  @Test
  public void initializesCorrectly() {
    // Check first 1K initial sizes.
    for (int i = 1; i < 1000; i++) {
      IntIntMap im = new IntIntMap(i);
      checkSize(im, 0);
    }
  }

  @Test
  public void failsOnInvalidSizes() {
    try {
      IntIntMap im;
      im = new IntIntMap(0);
      im = new IntIntMap(-1);
      im = new IntIntMap(Integer.MAX_VALUE);
      im = new IntIntMap(Integer.MIN_VALUE);
      im = new IntIntMap(Integer.MIN_VALUE + 1);
      im = new IntIntMap(1 << 29 + 1);
      Assert.fail("Illegal size should have thrown an exception.");
    } catch (RuntimeException e) {
      // Nothing to do
    }
  }

  @Test
  public void expandsCorrectly() {
    // Create maps with different sizes and add size * 10 elements to each.
    for (int i = 1; i < 100; i++) {
      IntIntMap im = new IntIntMap(i);
      // Insert i * 10 elements to each and confirm sizes
      int elements = i * 10;
      for (int j = 0; j < elements; j++) {
        im.put(j, j + 13);
      }
      for (int j = 0; j < elements; j++) {
        Assert.assertEquals(im.get(j), j + 13);
      }
      checkSize(im, elements);
    }
  }

  @Test
  public void putAddsAndUpdatesElementsCorrectly() {
    int span = 100;
    for (int i = 0; i < span; i++) {
      IntIntMap im = new IntIntMap();
      checkSpanInsertions(im, -i, i);
    }
    // Do the same, this time overwrite values as well
    IntIntMap im = new IntIntMap();
    for (int i = 0; i < span; i++) {
      checkSpanInsertions(im, -i, i);
      checkSpanInsertions(im, -i, i);
      checkSpanInsertions(im, -i, i);
    }
  }

  @Test
  public void removeRemovesCorrectly() {
    IntIntMap im = new IntIntMap();
    im.put(0,0);
    assertEquals(im.get(0), 0);
    im.remove(0);
    assertEquals(im.get(0), IntIntMap.NO_RESULT);
    assertEquals(im.size(), 0);
    // remove again works
    im.remove(0);
    assertEquals(im.get(0), IntIntMap.NO_RESULT);
    assertEquals(im.size(), 0);
    im.put(0, 1);
    assertEquals(im.size(), 1);
    assertEquals(im.get(0), 1);
  }

  @Test
  public void removeSpansWorksCorrectly() {
    IntIntMap im = new IntIntMap();
    insertSpan(im, 0, 99);
    removeSpan(im, 1, 98);
    assertEquals(im.size(), 2);
    checkSpanRemoved(im, 1, 98);
    insertSpan(im, 0, 99);
    assertEquals(im.size(), 100);
    checkSpan(im, 0, 99);
  }

  @Test
  public void removeSpansWorksCorrectly2() {
    IntIntMap im = new IntIntMap();
    int limit = 9999;
    insertSpan(im, 0, limit);
    int[] r = TestUtils.createRandomUintArray(1000, limit);
    for (int i : r) {
      im.remove(i);
    }
    for (int i : r) {
      assertEquals(im.get(i), IntIntMap.NO_RESULT);
    }
    insertSpan(im, 0, limit);
    checkSpan(im, 0, limit);
    removeSpan(im, 0, limit);
    assertEquals(im.size(), 0);
    insertSpan(im, -limit, limit);
    checkSpan(im, -limit, limit);
  }

  @Test
  public void survivesSimpleFuzzing() {
    List<int[]> fuzzLists = TestUtils.createFuzzingLists();
    for (int[] arr : fuzzLists) {
      IntIntMap im = new IntIntMap();
      for (int i = 0; i < arr.length; i++) {
        im.put(arr[i], arr[i] + 7);
        assertEquals(im.get(arr[i]), arr[i] + 7);
      }
    }

    IntIntMap im = new IntIntMap();
    for (int[] arr : fuzzLists) {
      for (int i = 0; i < arr.length; i++) {
        im.put(arr[i], arr[i] + 7);
        assertEquals(im.get(arr[i]), arr[i] + 7);
      }
    }
  }

  private void removeSpan(IntIntMap im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      im.remove(i);
    }
  }

  private void checkSpanRemoved(IntIntMap im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      assertEquals(im.get(i), IntIntMap.NO_RESULT);
    }
  }

  private void checkSpanInsertions(IntIntMap im, int start, int end) {
    insertSpan(im, start, end);
    // Expected size.
    int size = Math.abs(start) + Math.abs(end) + 1;
    assertEquals(size, im.size());
    checkSpan(im, start, end);
  }

  private void insertSpan(IntIntMap im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      im.put(i, i);
    }
  }

  private void checkSpan(IntIntMap im, int start, int end) {
    int spanStart = Math.min(start, end);
    int spanEnd = Math.max(start, end);
    for (int i = spanStart; i <= spanEnd; i++) {
      assertEquals(im.get(i), i);
    }
    // Check outside of span values do not exist in the map
    for (int i = spanStart - 1, idx = 0; idx < 100; i--, idx++) {
      Assert.assertEquals(IntIntMap.NO_RESULT, im.get(i));
    }
    for (int i = spanEnd + 1, idx = 0; idx < 100; i++, idx++) {
      Assert.assertEquals(IntIntMap.NO_RESULT, im.get(i));
    }
  }

  private void checkSize(IntIntMap m, int size) {
    assertEquals(size, m.size());
    assertTrue(m.capacity() > m.size());
    // Check capacity is 2^n
    assertTrue((m.capacity() & (m.capacity() - 1)) == 0);
  }

  @Test
  @Ignore("Not a unit test")
  public void testPerformance() {
    int[] arr = TestUtils.createRandomUintArray(1_000_000, 1<<29);
    long sum =0;
    int iter = 100;
    long start = System.currentTimeMillis();
    for (int i=0; i<iter; i++) {
      IntIntMap imap = new IntIntMap();
      for (int j=0; j<arr.length; j++) {
        imap.put(arr[j], arr[j] + 1);
      }
    }
    long elapsed = System.currentTimeMillis() - start;
    System.out.println("Creation: " + elapsed);

    IntIntMap imap = new IntIntMap();
    for (int j=0; j<arr.length; j++) {
      imap.put(arr[j], arr[j] + 1);
    }
    start = System.currentTimeMillis();
    for (int i=0; i<iter; i++) {
      for (int j=arr.length-1; j >=0; j--) {
        sum += imap.get(arr[j]);
      }
    }
    elapsed = System.currentTimeMillis() - start;
    System.out.println("Retrieval: " + elapsed);
    System.out.println("Val: " + sum);
  }

  @Test
  public void getTest2() {
    IntIntMap map = new IntIntMap();
    map.put(1, 2);
    Assert.assertEquals(2, map.get(1));
    Assert.assertEquals(IntIntMap.NO_RESULT, map.get(2));
    map.put(1, 3);
    Assert.assertEquals(3, map.get(1));

    map = new IntIntMap();
    for (int i = 0; i < 100000; i++) {
      map.put(i, i + 1);
    }
    for (int i = 0; i < 100000; i++) {
      Assert.assertEquals(i + 1, map.get(i));
    }
  }

  @Test
  public void removeTest2() {
    IntIntMap map = new IntIntMap();
    for (int i = 0; i < 10000; i++) {
      map.put(i, i + 1);
    }
    for (int i = 0; i < 10000; i += 3) {
      map.remove(i);
    }
    for (int i = 0; i < 10000; i += 3) {
      Assert.assertTrue(!map.containsKey(i));
    }
    for (int i = 0; i < 10000; i++) {
      map.put(i, i + 1);
    }
    for (int i = 0; i < 10000; i += 3) {
      Assert.assertTrue(map.containsKey(i));
    }
  }

  @Test
  @Ignore("Not a unit test")
  public void speedAgainstHashMap() {
    Random r = new Random();
    int[][] keyVals = new int[1000000][2];
    final int itCount = 10;
    for (int i = 0; i < keyVals.length; i++) {
      keyVals[i][0] = r.nextInt(500000);
      keyVals[i][1] = r.nextInt(5000) + 1;
    }
    Stopwatch sw = Stopwatch.createStarted();
    for (int j = 0; j < itCount; j++) {

      HashMap<Integer, Integer> map = new HashMap<>();

      for (int[] keyVal : keyVals) {
        map.put(keyVal[0], keyVal[1]);
      }

      for (int[] keyVal : keyVals) {
        map.get(keyVal[0]);
      }

      for (int[] keyVal : keyVals) {
        if(map.containsKey(keyVal)) {
          map.remove(keyVal[0]);
        }
      }
    }
    System.out.println("Map Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));

    IntIntMap countTable = new IntIntMap();
    sw = Stopwatch.createStarted();

    for (int j = 0; j < itCount; j++) {

      for (int[] keyVal : keyVals) {
        countTable.put(keyVal[0], keyVal[1]);
      }
      for (int[] keyVal : keyVals) {
        countTable.get(keyVal[0]);
      }
      for (int[] keyVal : keyVals) {
        if (countTable.containsKey(keyVal[0])) {
          countTable.remove(keyVal[0]);
        }
      }
    }
    System.out.println("IntIntMap Elapsed:" + sw.elapsed(TimeUnit.MILLISECONDS));
  }
  
  
}
