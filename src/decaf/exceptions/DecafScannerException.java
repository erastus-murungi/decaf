package decaf.exceptions;

import decaf.grammar.TokenPosition;

public class DecafScannerException extends DecafException {
    public DecafScannerException(TokenPosition tokenPosition, String errMessage) {
        super(tokenPosition.line(), tokenPosition.column(), errMessage);
    }
}
