package com.google.errorprone.bugpatterns;

import static com.google.errorprone.BugPattern.SeverityLevel.WARNING;
import static com.google.errorprone.matchers.Description.NO_MATCH;
import static com.google.errorprone.util.ASTHelpers.getType;

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
    summary = "HashMap keys should implement Comparable to themselves",
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
        && hashMapSupertype(getType(newClassTree), state) != null) {
      return NO_MATCH;
    }
    return describeIfNonComparableHashMapKey(getType(tree), tree, state);
  }

  private Description describeIfNonComparableHashMapKey(
      Type candidateType, Tree tree, VisitorState state) {
    Type hashMapType = hashMapSupertype(candidateType, state);
    if (hashMapType == null || hashMapType.getTypeArguments().isEmpty()) {
      return NO_MATCH;
    }

    Type keyType = hashMapType.getTypeArguments().get(0);
    if (isComparableToSelf(keyType, state)) {
      return NO_MATCH;
    }

    return buildDescription(tree)
        .setMessage(
            String.format(
                "HashMap key type '%s' does not implement Comparable. HashMap collision buckets"
                    + " can degrade when equal-hash keys are not comparable to themselves.",
                keyType))
        .build();
  }

  private static Type hashMapSupertype(Type type, VisitorState state) {
    Type hashMapType = state.getTypeFromString(HASH_MAP);
    return state.getTypes().asSuper(type, hashMapType.tsym);
  }

  private static boolean isComparableToSelf(Type type, VisitorState state) {
    Type upperBound = upperBound(type, state);
    Type comparableType =
        state.getTypes().asSuper(upperBound, state.getSymtab().comparableType.tsym);
    if (comparableType == null || comparableType.getTypeArguments().isEmpty()) {
      return false;
    }
    Type comparableArgument = comparableType.getTypeArguments().get(0);
    return state.getTypes().isSameType(comparableArgument, upperBound);
  }

  private static Type upperBound(Type type, VisitorState state) {
    return switch (type.getKind()) {
      case WILDCARD -> state.getTypes().wildUpperBound(type);
      default -> type;
    };
  }
}
