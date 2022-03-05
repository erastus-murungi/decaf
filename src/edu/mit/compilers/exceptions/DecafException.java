package edu.mit.compilers.exceptions;


public class DecafException extends Exception {
    final public int line;
    final public int column;

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
