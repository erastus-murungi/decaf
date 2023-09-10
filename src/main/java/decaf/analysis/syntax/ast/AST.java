package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.shared.AstVisitor;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public abstract class AST {
  public abstract List<Pair<String, AST>> getChildren();

  public abstract boolean isTerminal();

  public abstract <T> T accept(
      AstVisitor<T> astVisitor,
      Scope currentScope
  );

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  public abstract String getSourceCode();
}
