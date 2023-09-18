package decaf.analysis.syntax.ast;

import decaf.analysis.TokenPosition;

public abstract class AssignExpr extends AST {
  public Expression expression;

  public AssignExpr(
      TokenPosition tokenPosition,
      Expression expression
  ) {
      super(tokenPosition);
    this.expression = expression;
  }

  public abstract String getOperator();
}
