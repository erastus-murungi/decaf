package edu.mit.compilers.parse;

import edu.mit.compilers.utils.Pair;

import java.util.List;

public class AssignOpExpr extends AssignExpr {
    final AssignOp assignOp;
    final Expr expr;

    public AssignOpExpr(AssignOp assignOp, Expr expr) {
        this.assignOp = assignOp;
        this.expr = expr;
    }

    @Override
    public List<Pair<String, Node>> getChildren() {
        return List.of(new Pair<>("assignOp", assignOp), new Pair<>("expr", expr));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "AssignOpExpr{" + "assignOp=" + assignOp + ", expr=" + expr + '}';
    }
}
