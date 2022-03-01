package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;


public class HexLiteral extends IntLiteral {
    public HexLiteral(TokenPosition tokenPosition, String hexLiteral) {
        super(tokenPosition, hexLiteral);
    }
}