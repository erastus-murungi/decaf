package decaf.shared.env;


import static com.google.common.base.Preconditions.checkState;

import org.jetbrains.annotations.NotNull;

import java.util.TreeSet;

public class TypingContext {
  @NotNull
  public Scope globalScope;
  @NotNull
  public TreeSet<String> imports;

  public TypingContext(
      @NotNull Scope globalScope,
      @NotNull TreeSet<String> importDeclarations
  ) {
    this.globalScope = globalScope;
    this.imports = importDeclarations;
  }

  public boolean isGlobalVariable(String name) {
    return globalScope.containsKey(name);
  }

  public boolean isMethodParam(@NotNull String name, @NotNull Scope scope) {
    checkState(scope.target == Scope.For.Arguments);
    return scope.containsKey(name);
  }
}
