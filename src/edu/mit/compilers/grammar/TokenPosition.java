package edu.mit.compilers.grammar;

public class TokenPosition {
    final int line;
    final int column;
    final int offset;

    public TokenPosition(int line, int column, int offset) {
        this.line = line;
        this.column = column;
        this.offset = offset;
    }


    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    public int offset() {
        return offset;
    }

    @Override
    public String toString() {
        return "TokenPosition{" +
                "line=" + line +
                ", column=" + column +
                ", offset=" + offset +
                '}';
    }
}
