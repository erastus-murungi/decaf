package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class UnaryOpExpression extends Expression implements HasExpression {
  private final UnaryOperator unaryOperator;
  public Expression operand;

  public UnaryOpExpression(
      UnaryOperator unaryOperator,
      Expression operand
  ) {
    super(unaryOperator.tokenPosition);
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
