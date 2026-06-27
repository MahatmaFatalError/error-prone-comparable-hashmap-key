package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getType;
import static com.google.errorprone.util.ASTHelpers.isSameType;
import static com.google.errorprone.util.ASTHelpers.isSubtype;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker.NewClassTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.VariableTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;

@AutoService(BugChecker.class)
@BugPattern(
    name = "ComparableHashMapKey",
    summary = "HashMap keys should implement Comparable to keep collision buckets efficient",
    severity = WARNING)
public final class ComparableHashMapKey extends BugChecker
    implements NewClassTreeMatcher, VariableTreeMatcher {

  private static final String HASH_MAP = "java.util.HashMap";

  @Override
  public Description matchNewClass(NewClassTree tree, VisitorState state) {
    return describeIfNonComparableHashMapKey(getType(tree), tree, state);
  }

  @Override
  public Description matchVariable(VariableTree tree, VisitorState state) {
    if (tree.getInitializer() instanceof NewClassTree newClassTree
        && isHashMap(getType(newClassTree), state)) {
      return NO_MATCH;
    }
    return describeIfNonComparableHashMapKey(getType(tree), tree, state);
  }

  private Description describeIfNonComparableHashMapKey(
      Type candidateType, Tree tree, VisitorState state) {
    if (!isHashMap(candidateType, state)) {
      return NO_MATCH;
    }
    if (candidateType.getTypeArguments().isEmpty()) {
      return NO_MATCH;
    }

    Type keyType = candidateType.getTypeArguments().get(0);
    if (isComparable(keyType, state)) {
      return NO_MATCH;
    }

    return buildDescription(tree)
        .setMessage(
            String.format(
                "HashMap key type '%s' does not implement Comparable. HashMap collision buckets"
                    + " can degrade when equal-hash keys are not mutually comparable.",
                keyType))
        .build();
  }

  private static boolean isHashMap(Type type, VisitorState state) {
    Type hashMapType = state.getTypeFromString(HASH_MAP);
    return type != null
        && hashMapType != null
        && isSameType(state.getTypes().erasure(type), hashMapType, state);
  }

  private static boolean isComparable(Type type, VisitorState state) {
    Type upperBound = upperBound(type, state);
    return upperBound != null && isSubtype(upperBound, state.getSymtab().comparableType, state);
  }

  private static Type upperBound(Type type, VisitorState state) {
    if (type == null) {
      return null;
    }
    return switch (type.getKind()) {
      case WILDCARD -> state.getTypes().wildUpperBound(type);
      default -> type;
    };
  }
}
