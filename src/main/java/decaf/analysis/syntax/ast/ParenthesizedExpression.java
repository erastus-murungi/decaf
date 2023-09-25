package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.shared.AstVisitor;

import decaf.shared.Pair;

public class ParenthesizedExpression extends Expression implements HasExpression {
  private Expression expression;

  public ParenthesizedExpression(
      TokenPosition tokenPosition,
      Expression expression
  ) {
    super(tokenPosition);
    this.setExpression(expression);
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(new Pair<>(
        "expression",
        getExpression()
    ));
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "ParenthesizedExpression{" + "expression=" + getExpression() + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
            "(%s)",
            getExpression().getSourceCode()
    );
  }

  @Override
  public <ReturnType, InputType> ReturnType accept(
      AstVisitor<ReturnType, InputType> astVisitor,
      InputType input
  ) {
    return astVisitor.visit(
        this,
        input
    );
  }

  @Override
  public List<Expression> getExpressions() {
    return List.of(getExpression());
  }

  public Expression getExpression() {
    return expression;
  }

  @Override
  public void compareAndSwapExpression(
      Expression oldExpr,
      Expression newExpr
  ) {
    if (getExpression() == oldExpr)
      setExpression(newExpr);
  }

  public void setExpression(Expression expression) {
    this.expression = expression;
  }
}
