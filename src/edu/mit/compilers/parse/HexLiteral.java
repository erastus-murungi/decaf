package edu.mit.compilers.parse;

import edu.mit.compilers.grammar.TokenPosition;


public class HexLiteral extends IntLiteral {
    public HexLiteral(TokenPosition tokenPosition, String hexLiteral) {
        super(tokenPosition, hexLiteral);
    }
}