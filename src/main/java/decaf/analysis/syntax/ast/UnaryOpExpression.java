package decaf.analysis.syntax.ast;


import decaf.shared.AstVisitor;
import decaf.shared.Pair;

import java.util.List;

public class UnaryOpExpression extends Expression implements HasExpression {
  private final UnaryOperator unaryOperator;
  public Expression operand;

  public UnaryOpExpression(
      UnaryOperator unaryOperator,
      Expression operand
  ) {
    super(unaryOperator.getTokenPosition());
    this.unaryOperator = unaryOperator;
    this.operand = operand;
  }

  public UnaryOperator getUnaryOperator() {
    return unaryOperator;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(
        new Pair<>(
            "op",
            unaryOperator
        ),
        new Pair<>(
            "operand",
            operand
        )
    );
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "UnaryOpExpression{" + "op=" + unaryOperator + ", operand=" + operand + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
        "%s(%s)",
        unaryOperator.getSourceCode(),
        operand.getSourceCode()
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
    return List.of(operand);
  }

  @Override
  public void compareAndSwapExpression(
      Expression oldExpr,
      Expression newExpr
  ) {
    if (operand == oldExpr)
      operand = newExpr;
  }
}
