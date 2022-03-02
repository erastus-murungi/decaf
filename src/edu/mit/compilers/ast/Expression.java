package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;

public abstract class Expression extends AST {
    public TokenPosition tokenPosition;
    public BuiltinType builtinType;

    public Expression(TokenPosition tokenPosition) {
        this.tokenPosition = tokenPosition;
    }
}
