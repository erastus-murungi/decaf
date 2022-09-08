package edu.mit.compilers.asm.types;

import edu.mit.compilers.grammar.DecafScanner;

public enum X64BinaryInstructionType {
    addq,
    subq,
    andq,
    orq,
    cmpq,
    imulq,
    xorl,
    leaq,
    movq,
    movzbq, xorq;

    public static X64BinaryInstructionType getX64BinaryInstruction(String operator) {
        switch (operator) {
            case DecafScanner.PLUS:
                return addq;
            case DecafScanner.MINUS:
                return subq;
            case DecafScanner.CONDITIONAL_AND:
                return andq;
            case DecafScanner.CONDITIONAL_OR:
                return orq;
            case DecafScanner.MULTIPLY:
                return imulq;
        }
        throw new IllegalStateException("operator " + operator + " not found");
    }

    @Override
    public String toString() {
        String s = super.toString();
        if (s.length() < 4) {
            s = s + " ".repeat(4 - s.length());
        }
        return s;
    }
}

