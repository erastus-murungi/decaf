package edu.mit.compilers.parse;

import edu.mit.compilers.utils.Pair;

import java.util.List;

public class UnaryOpExpr extends Expr {
    final UnaryOp op;
    final Expr operand;

    public UnaryOpExpr(UnaryOp op, Expr operand) {
        this.op = op;
        this.operand = operand;
    }

    @Override
    public List<Pair<String, Node>> getChildren() {
        return List.of(new Pair<>("op", op), new Pair<>("operand", operand));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "UnaryOpExpr{" + "op=" + op + ", operand=" + operand + '}';
    }
}
