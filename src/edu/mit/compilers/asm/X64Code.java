package edu.mit.compilers.asm;

public class X64Code {
    String line;
    String comment;

    public X64Code(String line) {
        this.line = line;
    }

    public X64Code(String line, String comment) {
        this.line = line;
        this.comment = comment;
    }

    @Override
    public String toString() {
        if (comment != null) {
            return String.format("%s%s # %s", line, "\t", comment);
        }
        return line;
    }
}
