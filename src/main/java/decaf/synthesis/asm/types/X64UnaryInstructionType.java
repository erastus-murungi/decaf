package decaf.synthesis.asm.types;


import decaf.analysis.lexical.Scanner;

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
      case Scanner.GEQ:
        return setge;
      case Scanner.GT:
        return setg;
      case Scanner.LT:
        return setl;
      case Scanner.LEQ:
        return setle;
      case Scanner.EQ:
        return sete;
      case Scanner.NEQ:
        return setne;
    }
    throw new IllegalStateException("operator " + operator + " not found");
  }


  public static X64UnaryInstructionType getCorrectJumpIfFalseInstruction(String operator) {
    switch (operator) {
      case Scanner.LT:
        return jge;
      case Scanner.GT:
        return jle;
      case Scanner.GEQ:
        return jl;
      case Scanner.LEQ:
        return jg;
      case Scanner.EQ:
        return jne;
      case Scanner.NEQ:
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
