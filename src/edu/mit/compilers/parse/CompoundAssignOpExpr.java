package edu.mit.compilers.parse;

import edu.mit.compilers.utils.Pair;

import java.util.List;

public class CompoundAssignOpExpr extends AssignExpr {
    final CompoundAssignOp compoundAssignOp;
    final Expr expr;

    public CompoundAssignOpExpr(CompoundAssignOp compoundAssignOp, Expr expr) {
        this.compoundAssignOp = compoundAssignOp;
        this.expr = expr;
    }

    @Override
    public List<Pair<String, Node>> getChildren() {
        return List.of(new Pair<>("compoundAssign", compoundAssignOp), new Pair<>("expr", expr));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }


    @Override
    public String toString() {
        return "CompoundAssignOpExpr{" + "compoundAssignOp=" + compoundAssignOp + ", expr=" + expr + '}';
    }
}
