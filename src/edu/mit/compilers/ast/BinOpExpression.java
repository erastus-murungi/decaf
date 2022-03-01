package edu.mit.compilers.ast;

import edu.mit.compilers.utils.Pair;

import java.util.List;

public class BinOpExpression extends Expression {
  final BinOperator op;
  final Pair<Expression, Expression> operands;

  public BinOpExpression(Expression left, BinOperator binaryOp, Expression right) {
    this.operands = new Pair<>(left, right);
    this.op = binaryOp;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(
        new Pair<>("op", op),
        new Pair<>("operand", operands.first()),
        new Pair<>("operand", operands.second()));
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "BinOpExpression{" + "op=" + op + ", operands=" + operands + '}';
  }
}
