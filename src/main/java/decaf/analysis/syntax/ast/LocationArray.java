package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class LocationArray extends Location implements HasExpression {
  public Expression expression;

  public LocationArray(
      RValue RValue,
      Expression expression
  ) {
    super(RValue);
    this.expression = expression;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(
        new Pair<>(
            "id",
            rValue
        ),
        new Pair<>(
            "expression",
            expression
        )
    );
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public <T> T accept(
      AstVisitor<T> astVisitor,
      Scope currentScope
  ) {
    return astVisitor.visit(
        this,
        currentScope
    );
  }

  @Override
  public String toString() {
    return "LocationArray{" + "name=" + rValue + ", expression=" + expression + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
        "%s[%s]",
        rValue.getSourceCode(),
        expression.getSourceCode()
    );
  }

  @Override
  public List<Expression> getExpression() {
    return List.of(expression);
  }

  @Override
  public void compareAndSwapExpression(
      Expression oldExpr,
      Expression newExpr
  ) {
    if (expression == oldExpr)
      expression = newExpr;
  }
}
