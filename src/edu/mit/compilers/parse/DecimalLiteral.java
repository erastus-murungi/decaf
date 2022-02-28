package edu.mit.compilers.parse;

import edu.mit.compilers.grammar.TokenPosition;

public class DecimalLiteral extends IntLiteral {
    public DecimalLiteral(TokenPosition tokenPosition, String literalToken) {
        super(tokenPosition, literalToken);
    }
}