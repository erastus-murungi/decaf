package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;

public abstract class Literal extends Expression {
    public String literal;

    public Literal(TokenPosition tokenPosition, String literal) {
        super(tokenPosition);
        this.literal = literal;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
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
