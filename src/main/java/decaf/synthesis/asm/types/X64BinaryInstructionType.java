package decaf.synthesis.asm.types;


import decaf.analysis.lexical.Scanner;

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
      case Scanner.PLUS:
        return addq;
      case Scanner.MINUS:
        return subq;
      case Scanner.CONDITIONAL_AND:
        return andq;
      case Scanner.CONDITIONAL_OR:
        return orq;
      case Scanner.MULTIPLY:
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

