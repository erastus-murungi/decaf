package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.shared.AstVisitor;
import decaf.shared.Pair;
import org.jetbrains.annotations.NotNull;

public class AssignOpExpr extends AssignExpr implements HasExpression {
  @NotNull private final AssignOperator assignOp;
  @NotNull private Expression expression;

  public AssignOpExpr(
      @NotNull TokenPosition tokenPosition,
      @NotNull AssignOperator assignOp,
      @NotNull Expression expression
  ) {
    super(
        tokenPosition,
        expression
    );
    this.assignOp = assignOp;
    this.expression = expression;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(
        new Pair<>(
            "assignOp",
            getAssignOp()
        ),
        new Pair<>(
            "expression",
            getExpression()
        )
    );
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "AssignOpExpr{" + "assignOp=" + getAssignOp() + ", expression=" + getExpression() + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
            "%s %s",
            getAssignOp().getSourceCode(),
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
  public String getOperator() {
    return getAssignOp().label;
  }

  @Override
  public List<Expression> getExpressions() {
    return List.of(getExpression());
  }

  @Override
  public void compareAndSwapExpression(
      Expression oldExpr,
      Expression newExpr
  ) {
    if (getExpression() == oldExpr)
      setExpression(newExpr);
  }

  public void setExpression(@NotNull Expression expression) {
    this.expression = expression;
  }

  public @NotNull Expression getExpression() {
    return expression;
  }

  public @NotNull AssignOperator getAssignOp() {
    return assignOp;
  }
}
