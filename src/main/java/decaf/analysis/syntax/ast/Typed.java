package decaf.analysis.syntax.ast;

import org.jetbrains.annotations.NotNull;

public interface Typed<T extends AST> {
  @NotNull Type getType();
  T setType(@NotNull Type type);
}
