package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.shared.AstVisitor;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class CompoundAssignOpExpr extends AssignExpr implements HasExpression {
  public final CompoundAssignOperator compoundAssignOp;

  public CompoundAssignOpExpr(
      TokenPosition tokenPosition,
      CompoundAssignOperator compoundAssignOp,
      Expression expression
  ) {
    super(
        tokenPosition,
        expression
    );
    this.compoundAssignOp = compoundAssignOp;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(
        new Pair<>(
            "compoundAssign",
            compoundAssignOp
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
    return "CompoundAssignOpExpr{" + "compoundAssignOp=" + compoundAssignOp + ", expression=" + expression + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
        "%s %s",
        compoundAssignOp.getSourceCode(),
        expression.getSourceCode()
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
  public String getOperator() {
    return compoundAssignOp.label;
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
