package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;

public abstract class Statement extends AST {
    final TokenPosition tokenPosition;

    public Statement(TokenPosition tokenPosition) {
        this.tokenPosition = tokenPosition;
    }
}
