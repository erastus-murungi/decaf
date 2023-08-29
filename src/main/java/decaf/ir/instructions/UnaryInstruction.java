package decaf.ir.instructions;

import org.jetbrains.annotations.NotNull;

import decaf.ir.values.Value;

public class UnaryInstruction extends Instruction {
  public enum Op {
    NEGATE,
    NOT,
    COPY,
  }
  @NotNull
  public final Value operand;
  @NotNull
  public final Op op;

  public UnaryInstruction(@NotNull Value operand, @NotNull Op op) {
    this.operand = operand;
    this.op = op;
  }

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
    return false;
  }
}
