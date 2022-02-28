package edu.mit.compilers.parse;

import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;

public class ExprArg extends MethodCallArg {
    final Expr expr;
    @Override
    public List<Pair<String, Node>> getChildren() {
        return Collections.singletonList(new Pair<>("expr", expr));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    public ExprArg(Expr expr) {
        this.expr = expr;
    }

    @Override
    public String toString() {
        return "ExprArg{" + "expr=" + expr + '}';
    }
}
