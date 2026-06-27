# Comparable HashMap Key - Error Prone Plugin

An [Error Prone](https://errorprone.info/) checker that flags `java.util.HashMap`
uses whose key type does not implement `Comparable`.

The motivation is the HashMap collision case described in
[Java map keys should always be Comparable](https://dev.to/carey/java-map-keys-should-always-be-comparable-2c1b):
when many keys share the same hash, Java can treeify a bucket, but lookups are
more predictable when keys are mutually comparable.

## What it catches

```java
final class Key {}

HashMap<Key, String> byKey = new HashMap<>();
Map<Key, String> byKeyInterface = new HashMap<>();
```

## What it allows

```java
HashMap<String, String> byName = new HashMap<>();

final class Key implements Comparable<Key> {
  @Override
  public int compareTo(Key other) {
    return 0;
  }
}

HashMap<Key, String> byKey = new HashMap<>();
```

Raw `HashMap` declarations are ignored because there is no static key type to
check.

## Usage

Add this artifact to the same Error Prone setup as `error_prone_core`.

```xml
<path>
  <groupId>io.github.mahatmafatalerror</groupId>
  <artifactId>error-prone-comparable-hashmap-key</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</path>
```

Suppress individual cases with:

```java
@SuppressWarnings("ComparableHashMapKey")
```

## Building

```bash
./mvnw verify
```
