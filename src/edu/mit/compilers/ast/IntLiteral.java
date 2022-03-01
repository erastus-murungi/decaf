package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;

public abstract class IntLiteral extends Literal {

    public IntLiteral(TokenPosition tokenPosition, String literalToken) {
        super(tokenPosition, literalToken);
    }
}