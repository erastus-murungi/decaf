package edu.mit.compilers.asm;

public class X64Code {
    String line;

    public X64Code(String line) {
        this.line = line;
    }

    @Override
    public String toString() {
        return line;
    }
}
