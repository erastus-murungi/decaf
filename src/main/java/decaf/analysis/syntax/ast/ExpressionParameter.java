package decaf.analysis.syntax.ast;


import java.util.Collections;
import java.util.List;

import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class ExpressionParameter extends MethodCallParameter implements HasExpression {
  public Expression expression;

  public ExpressionParameter(Expression expression) {
    this.expression = expression;
  }

  @Override
  public Type getType() {
    return expression.getType();
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
      AstVisitor<T> ASTVisitor,
      Scope curScope
  ) {
    return ASTVisitor.visit(
        this,
        curScope
    );
  }

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return codegenAstVisitor.visit(
        this,
        resultLocation
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
