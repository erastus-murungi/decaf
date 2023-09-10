package decaf.analysis.syntax.ast;

import decaf.analysis.TokenPosition;

public abstract class Expression extends AST implements Typed<Expression> {
  public TokenPosition tokenPosition;
  public Expression(TokenPosition tokenPosition) {
    this.tokenPosition = tokenPosition;
  }
}
