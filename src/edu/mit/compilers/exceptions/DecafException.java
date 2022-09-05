package edu.mit.compilers.exceptions;


import edu.mit.compilers.grammar.TokenPosition;

public class DecafException extends Exception {
    final public int line;
    final public int column;

    public DecafException(int line, int column, String errMessage) {
        super(errMessage);
        this.line = line;
        this.column = column;
    }

    public DecafException(TokenPosition tokenPosition, String errMessage) {
        super(errMessage);
        line = tokenPosition.line();
        column = tokenPosition.column();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (" + "line=" + line + ", " + "column=" + column + ')' + ": " + getMessage();
    }
}
