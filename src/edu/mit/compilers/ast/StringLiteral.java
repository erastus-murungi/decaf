package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;

public class StringLiteral extends MethodCallParameter {
    final TokenPosition tokenPosition;
    final String literal;

    public StringLiteral(TokenPosition tokenPosition, String literal) {
        this.tokenPosition = tokenPosition;
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
        return "StringLiteral{" + literal + "}";

    }
}