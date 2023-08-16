package decaf.analysis.syntax.ast;


import java.util.Collections;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.shared.Pair;

public abstract class Literal extends Expression {
  public String literal;

  public Literal(
      TokenPosition tokenPosition,
      String literal
  ) {
    super(tokenPosition);
    this.literal = literal;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return Collections.emptyList();
  }

  @Override
  public boolean isTerminal() {
    return true;
  }

  @Override
  public String toString() {
    return this.getClass()
               .getSimpleName() + "{'" + literal + '\'' + '}';
  }

  @Override
  public String getSourceCode() {
    return literal;
  }
}
