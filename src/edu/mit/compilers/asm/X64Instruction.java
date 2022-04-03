package edu.mit.compilers.asm;

import edu.mit.compilers.grammar.DecafScanner;

public enum X64Instruction {
    andq, setge, add, cmpq ,sub, sete, shl, jne, ret, cqto, popq, and, cmp, jnz, mov, xor, setle, setg, subq, align, setne, inc, jge, test, call, string, movzx, dec, custom, shr, movl, jz, addq, not, setl, imulq, jmp, movq, orq, newline, or, idivq, neg, leaq, pushq, jl, leave, movzbq, jle, jg, je, andl;

    public static X64Instruction getX64OperatorCode(String operator) {
        switch (operator) {
            case DecafScanner.PLUS:
                return addq;
            case DecafScanner.MINUS:
                return subq;
            case DecafScanner.DIVIDE:
                return idivq;
            case DecafScanner.GEQ:
            case DecafScanner.GT:
            case DecafScanner.LT:
            case DecafScanner.LEQ:
            case DecafScanner.EQ:
            case DecafScanner.NEQ:
                return cmp;
            case DecafScanner.MULTIPLY:
                return imulq;
        }
        throw new IllegalStateException("operator " + operator + " not found");
    }

    public static X64Instruction getCorrectComparisonSetInstruction(String operator) {
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

    public static X64Instruction getCorrectJumpIfFalseInstruction(String operator) {
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
