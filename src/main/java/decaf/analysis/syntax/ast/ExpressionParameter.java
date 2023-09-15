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
  public <T> T accept(
      AstVisitor<T> astVisitor,
      Scope curScope
  ) {
    return astVisitor.visit(
        this,
        curScope
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

  @Override
  public @NotNull Type getType() {
    return expression.getType();
  }

  @Override
  public void setType(@NotNull Type type) {
    expression.setType(type);
  }
}
