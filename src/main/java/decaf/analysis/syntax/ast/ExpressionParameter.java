package decaf.analysis.syntax.ast;


import decaf.analysis.syntax.ast.types.Type;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class ExpressionParameter extends ActualArgument implements HasExpression, Typed {
  public Expression expression;

  public ExpressionParameter(Expression expression) {
    super(expression.getTokenPosition());
    this.expression = expression;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return Collections.singletonList(new Pair<>(
        "expression",
        expression
    ));
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "ExpressionParameter{" + "expression=" + expression + '}';
  }

  @Override
  public String getSourceCode() {
    return expression.getSourceCode();
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

  @Override
  public @NotNull Type getType() {
    return expression.getType();
  }

  @Override
  public void setType(@NotNull Type type) {
    expression.setType(type);
  }
}
