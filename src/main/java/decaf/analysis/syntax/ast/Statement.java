package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import org.jetbrains.annotations.NotNull;

public abstract class Statement extends AST {
  public Statement(@NotNull TokenPosition tokenPosition) {
    super(tokenPosition);
  }
}
