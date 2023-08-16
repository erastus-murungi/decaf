package decaf.analysis.syntax.ast;

import decaf.analysis.TokenPosition;

public abstract class AssignExpr extends AST {
  public TokenPosition tokenPosition;
  public Expression expression;

  public AssignExpr(
      TokenPosition tokenPosition,
      Expression expression
  ) {
    this.tokenPosition = tokenPosition;
    this.expression = expression;
  }

  @Override
  public Type getType() {
    return expression.getType();
  }

  public abstract String getOperator();
}
