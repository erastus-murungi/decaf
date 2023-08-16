package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class AssignOpExpr extends AssignExpr implements HasExpression {
  final public AssignOperator assignOp;
  public Expression expression;

  public AssignOpExpr(
      TokenPosition tokenPosition,
      AssignOperator assignOp,
      Expression expression
  ) {
    super(
        tokenPosition,
        expression
    );
    this.assignOp = assignOp;
    this.expression = expression;
  }

  @Override
  public Type getType() {
    return Type.Undefined;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(
        new Pair<>(
            "assignOp",
            assignOp
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
  public String toString() {
    return "AssignOpExpr{" + "assignOp=" + assignOp + ", expression=" + expression + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
        "%s %s",
        assignOp.getSourceCode(),
        expression.getSourceCode()
    );
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
    return null;
  }

  @Override
  public String getOperator() {
    return assignOp.label;
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
