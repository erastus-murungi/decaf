package edu.mit.compilers.parse;

import edu.mit.compilers.utils.Pair;

import java.util.List;

public class ParenthesizedExpr extends Expr {
    final Expr expr;

    public ParenthesizedExpr(Expr expr) {
        this.expr = expr;
    }

    @Override
    public List<Pair<String, Node>> getChildren() {
        return List.of(new Pair<>("expr", expr));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "ParenthesizedExpr{" + "expr=" + expr + '}';
    }
}
