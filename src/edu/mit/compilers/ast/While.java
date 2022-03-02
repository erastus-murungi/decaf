package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class While extends Statement {
    final Expression test;
    final Block body;

    public While(TokenPosition tokenPosition, Expression test, Block body) {
        super(tokenPosition);
        this.test = test;
        this.body = body;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
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