package edu.mit.compilers.parse;

import edu.mit.compilers.utils.Pair;

import java.util.List;

public class BinOpExpr extends Expr {
  final BinOp op;
  final Pair<Expr, Expr> operands;

  public BinOpExpr(Expr left, BinOp binaryOp, Expr right) {
    this.operands = new Pair<>(left, right);
    this.op = binaryOp;
  }

  @Override
  public List<Pair<String, Node>> getChildren() {
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
    return "BinOpExpr{" + "op=" + op + ", operands=" + operands + '}';
  }
}
