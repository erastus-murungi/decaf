package edu.mit.compilers.exceptions;

import edu.mit.compilers.grammar.TokenPosition;

public class DecafScannerException extends DecafException {
    public DecafScannerException(TokenPosition tokenPosition, String errMessage) {
        super(tokenPosition.line(), tokenPosition.column(), errMessage);
    }
}
