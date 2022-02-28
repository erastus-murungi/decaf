package edu.mit.compilers.exceptions;


public class DecafException extends Exception {
    final int line;
    final int column;

    public DecafException(int line, int column, String errMessage) {
        super(errMessage);
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " (" + "line=" + line  + ", " +"column=" + column + ')' + ": " + getMessage();
    }
}
