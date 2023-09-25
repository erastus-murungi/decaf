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
  public <ReturnType, InputType> ReturnType accept(AstVisitor<ReturnType, InputType> astVisitor, InputType input) {
    return astVisitor.visit(this, input);
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
  public List<Expression> getExpressions() {
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
