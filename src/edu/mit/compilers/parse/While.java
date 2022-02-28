package edu.mit.compilers.parse;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class While extends Statement {
    final Expr test;
    final Block body;

    public While(TokenPosition tokenPosition, Expr test, Block body) {
        super(tokenPosition);
        this.test = test;
        this.body = body;
    }

    @Override
    public List<Pair<String, Node>> getChildren() {
        return List.of(new Pair<>("test", test), new Pair<>("body", body));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "While{" + "test=" + test + ", body=" + body + '}';
    }
}
