package edu.mit.compilers.parse;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;

public abstract class Literal extends Expr {
    final TokenPosition tokenPosition;
    final String literal;

    public Literal(TokenPosition tokenPosition, String literal) {
        this.tokenPosition = tokenPosition;
        this.literal = literal;
    }

    @Override
    public List<Pair<String, Node>> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{'" + literal + '\'' + '}';
    }
}
