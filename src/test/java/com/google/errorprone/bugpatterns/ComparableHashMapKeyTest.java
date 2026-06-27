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
  void comparableHashMapDeclarationWithoutInitializer_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;

            class Test {
              HashMap<String, Integer> names;
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
  void hashMapFromMethodReturnWithNonComparableKey_finding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;

            final class Key {}

            class Test {
              HashMap<Key, Integer> create() {
                return null;
              }

              void test() {
                // BUG: Diagnostic contains: HashMap key type 'Key' does not implement Comparable
                HashMap<Key, Integer> keys = create();
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
  void enumKey_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;

            enum Key {
              ONE
            }

            class Test {
              HashMap<Key, Integer> keys = new HashMap<>();
            }
            """)
        .doTest();
  }

  @Test
  void comparableToDifferentType_finding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;

            final class Other {}

            final class Key implements Comparable<Other> {
              @Override
              public int compareTo(Other other) {
                return 0;
              }
            }

            class Test {
              // BUG: Diagnostic contains: HashMap key type 'Key' does not implement Comparable
              HashMap<Key, Integer> keys;
            }
            """)
        .doTest();
  }

  @Test
  void comparableToObject_finding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;

            final class Key implements Comparable<Object> {
              @Override
              public int compareTo(Object other) {
                return 0;
              }
            }

            class Test {
              // BUG: Diagnostic contains: HashMap key type 'Key' does not implement Comparable
              HashMap<Key, Integer> keys;
            }
            """)
        .doTest();
  }

  @Test
  void rawComparable_finding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;

            @SuppressWarnings("rawtypes")
            final class Key implements Comparable {
              @Override
              public int compareTo(Object other) {
                return 0;
              }
            }

            class Test {
              // BUG: Diagnostic contains: HashMap key type 'Key' does not implement Comparable
              HashMap<Key, Integer> keys;
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
  void upperBoundedWildcardWithSelfComparableBound_noFinding() {
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
              HashMap<? extends Key, Integer> keys;
            }
            """)
        .doTest();
  }

  @Test
  void upperBoundedWildcardWithUnknownSelfComparableType_finding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;

            class Test {
              // BUG: Diagnostic contains: does not implement Comparable
              HashMap<? extends Comparable<?>, Integer> keys;
            }
            """)
        .doTest();
  }

  @Test
  void lowerBoundedWildcard_finding() {
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
              // BUG: Diagnostic contains: does not implement Comparable
              HashMap<? super Key, Integer> keys;
            }
            """)
        .doTest();
  }

  @Test
  void fullyQualifiedHashMap_finding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            final class Key {}

            class Test {
              // BUG: Diagnostic contains: HashMap key type 'Key' does not implement Comparable
              java.util.HashMap<Key, Integer> keys;
            }
            """)
        .doTest();
  }

  @Test
  void constructorWithExplicitTypeArguments_finding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;

            final class Key {}

            class Test {
              void test() {
                // BUG: Diagnostic contains: HashMap key type 'Key' does not implement Comparable
                new HashMap<Key, Integer>(16);
              }
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
  void nonHashMapNewClass_noFinding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.ArrayList;

            final class Key {}

            class Test {
              ArrayList<Key> keys = new ArrayList<>();
            }
            """)
        .doTest();
  }

  @Test
  void linkedHashMapWithNonComparableKey_finding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.LinkedHashMap;

            final class Key {}

            class Test {
              // BUG: Diagnostic contains: HashMap key type 'Key' does not implement Comparable
              LinkedHashMap<Key, Integer> keys = new LinkedHashMap<>();
            }
            """)
        .doTest();
  }

  @Test
  void anonymousHashMapSubclassWithNonComparableKey_finding() {
    helper
        .addSourceLines(
            "Test.java",
            """
            import java.util.HashMap;

            final class Key {}

            class Test {
              void test() {
                // BUG: Diagnostic contains: HashMap key type 'Key' does not implement Comparable
                new HashMap<Key, Integer>() {};
              }
            }
            """)
        .doTest();
  }

  @Test
  void customHashMapSubclassWithComparableKey_noFinding() {
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

            final class KeyMap extends HashMap<Key, Integer> {}

            class Test {
              KeyMap keys = new KeyMap();
            }
            """)
        .doTest();
  }
}
