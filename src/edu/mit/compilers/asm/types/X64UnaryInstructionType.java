package edu.mit.compilers.asm.types;

import edu.mit.compilers.grammar.DecafScanner;

public enum X64UnaryInstructionType {
    popq,
    pushq,
    jmp,
    callq,
    idivq,
    setge,
    setg,
    setl,
    setle,
    sete,
    setne,
    neg,
    je,
    jge,
    jle,
    jl,
    jg,
    jne;

    public static X64UnaryInstructionType getCorrectComparisonSetInstruction(String operator) {
        switch (operator) {
            case DecafScanner.GEQ:
                return setge;
            case DecafScanner.GT:
                return setg;
            case DecafScanner.LT:
                return setl;
            case DecafScanner.LEQ:
                return setle;
            case DecafScanner.EQ:
                return sete;
            case DecafScanner.NEQ:
                return setne;
        }
        throw new IllegalStateException("operator " + operator + " not found");
    }


    public static X64UnaryInstructionType getCorrectJumpIfFalseInstruction(String operator) {
        switch (operator) {
            case DecafScanner.LT:
                return jge;
            case DecafScanner.GT:
                return jle;
            case DecafScanner.GEQ:
                return jl;
            case DecafScanner.LEQ:
                return jg;
            case DecafScanner.EQ:
                return jne;
            case DecafScanner.NEQ:
                return je;
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
