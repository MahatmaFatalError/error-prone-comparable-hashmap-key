package com.google.errorprone.bugpatterns;

import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;

class ComparableHashMapKeyTest {

  private final CompilationTestHelper helper =
      CompilationTestHelper.newInstance(ComparableHashMapKey.class, getClass());

  @Test
  void newHashMapWithComparableKey_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;

            class Test {
              HashMap<String, Integer> names = new HashMap<>();
            }
            """)
        .doTest();
  }

  @Test
  void newHashMapWithNonComparableKey_finding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;

            final class Key {}

            class Test {
              void test() {
                // BUG: Diagnostic contains: HashMap key type 'Key' does not implement Comparable
                HashMap<Key, Integer> keys = new HashMap<>();
              }
            }
            """)
        .doTest();
  }

  @Test
  void diamondInferredFromMapVariable_finding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;
            import java.util.Map;

            final class Key {}

            class Test {
              void test() {
                // BUG: Diagnostic contains: HashMap key type 'Key' does not implement Comparable
                Map<Key, Integer> keys = new HashMap<>();
              }
            }
            """)
        .doTest();
  }

  @Test
  void declarationWithoutInitializer_finding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;

            final class Key {}

            class Test {
              // BUG: Diagnostic contains: HashMap key type 'Key' does not implement Comparable
              HashMap<Key, Integer> keys;
            }
            """)
        .doTest();
  }

  @Test
  void customComparableKey_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;

            final class Key implements Comparable<Key> {
              @Override
              public int compareTo(Key other) {
                return 0;
              }
            }

            class Test {
              void test() {
                HashMap<Key, Integer> keys = new HashMap<>();
              }
            }
            """)
        .doTest();
  }

  @Test
  void boundedTypeParameter_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;

            class Test<K extends Comparable<K>> {
              HashMap<K, Integer> keys = new HashMap<>();
            }
            """)
        .doTest();
  }

  @Test
  void unboundedTypeParameter_finding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;

            class Test<K> {
              // BUG: Diagnostic contains: HashMap key type 'K' does not implement Comparable
              HashMap<K, Integer> keys;
            }
            """)
        .doTest();
  }

  @Test
  void upperBoundedWildcard_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;

            class Test {
              HashMap<? extends Comparable<?>, Integer> keys;
            }
            """)
        .doTest();
  }

  @Test
  void rawHashMap_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;

            class Test {
              @SuppressWarnings("rawtypes")
              HashMap keys = new HashMap();
            }
            """)
        .doTest();
  }

  @Test
  void linkedHashMap_isNotFlagged() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.LinkedHashMap;

            final class Key {}

            class Test {
              LinkedHashMap<Key, Integer> keys = new LinkedHashMap<>();
            }
            """)
        .doTest();
  }
}
