package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;

public class CharLiteral extends Literal {
    public CharLiteral(TokenPosition tokenPosition, String literal) {
        super(tokenPosition, literal);
    }
}
