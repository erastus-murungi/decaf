package decaf.shared.env;


import static com.google.common.base.Preconditions.checkState;

import org.jetbrains.annotations.NotNull;

public class TypingContext {
  @NotNull
  public Scope globalScope;
  public TypingContext(
      @NotNull Scope globalScope
  ) {
    this.globalScope = globalScope;
  }

  public boolean isGlobalVariable(String name) {
    return globalScope.containsKey(name);
  }

  public boolean isMethodParam(@NotNull String name, @NotNull Scope scope) {
    checkState(scope.target == Scope.For.Arguments);
    return scope.containsKey(name);
  }
}
