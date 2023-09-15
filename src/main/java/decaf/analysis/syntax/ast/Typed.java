package decaf.analysis.syntax.ast;

import decaf.analysis.syntax.ast.types.Type;
import org.jetbrains.annotations.NotNull;

public interface Typed {
  @NotNull Type getType();
  void setType(@NotNull Type type);
}
