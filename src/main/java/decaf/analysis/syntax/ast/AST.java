package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.shared.AstVisitor;
import decaf.shared.Pair;
import decaf.shared.env.Scope;
import org.jetbrains.annotations.NotNull;

public abstract class AST {
  @NotNull final private TokenPosition tokenPosition;

  protected AST(@NotNull TokenPosition tokenPosition) {
    this.tokenPosition = tokenPosition;
  }

  public abstract List<Pair<String, AST>> getChildren();

  public abstract boolean isTerminal();

  public abstract <ReturnType, InputType> ReturnType accept(AstVisitor<ReturnType, InputType> astVisitor, InputType input);

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  public abstract String getSourceCode();

  public @NotNull TokenPosition getTokenPosition() {
    return tokenPosition;
  }
}
