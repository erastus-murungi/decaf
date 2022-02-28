package edu.mit.compilers.parse;

import edu.mit.compilers.grammar.TokenPosition;

public abstract class Statement extends Node {
    final TokenPosition tokenPosition;

    public Statement(TokenPosition tokenPosition) {
        this.tokenPosition = tokenPosition;
    }
}
