package edu.mit.compilers.exceptions;

import edu.mit.compilers.grammar.Token;

public class DecafParserException extends DecafException {

    public DecafParserException(Token token, String message) {
        super(token.tokenPosition().line(), token.tokenPosition().column(), message);
    }
}
