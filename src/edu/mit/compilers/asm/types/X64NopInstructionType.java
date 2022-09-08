package edu.mit.compilers.asm.types;

public enum X64NopInstructionType {
    cqto, retq;

    @Override
    public String toString() {
        String s = super.toString();
        if (s.length() < 4) {
            s = s + " ".repeat(4 - s.length());
        }
        return s;
    }
}
