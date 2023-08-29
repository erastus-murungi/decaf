package decaf.ir.instructions;

import org.jetbrains.annotations.NotNull;

import decaf.ir.values.Value;

public class BinaryInstruction extends Instruction {
  public enum Op {
    ADD,
    SUB,
    MUL,
    DIV,
    MOD,
    SHL,
    SHR,
    AND,
    OR,
    XOR,
    EQ,
    NE,
    LT,
    LE,
    GT,
    GE,
  }

  public BinaryInstruction(@NotNull Op op, @NotNull Value lhs, @NotNull Value rhs) {
    this.op = op;
    this.lhs = lhs;
    this.rhs = rhs;
  }

  @NotNull
  private final Op op;
  @NotNull
  private final Value lhs;
  @NotNull
  private final Value rhs;

  @Override
  public String prettyPrint() {
    return null;
  }

  @Override
  public String toString() {
    return null;
  }

  @Override
  public String prettyPrintColored() {
    return null;
  }

  @Override
  public <T> boolean isWellFormed(T neededContext) throws InstructionMalformed {
    throw new InstructionMalformed();
  }
}
