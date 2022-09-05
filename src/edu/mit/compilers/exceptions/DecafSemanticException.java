package edu.mit.compilers.exceptions;

import edu.mit.compilers.grammar.TokenPosition;

public class DecafSemanticException extends DecafScannerException {
    public DecafSemanticException(TokenPosition tokenPosition, String errMessage) {
        super(tokenPosition, errMessage);
    }
}
