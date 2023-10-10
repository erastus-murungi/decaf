package decaf.ir.instructions;

import org.jetbrains.annotations.NotNull;

import decaf.ir.values.IrValue;

import java.util.List;

public class UnaryInstruction extends Instruction {
  public enum Op {
    NEGATE,
    NOT,
    COPY,
  }
  @NotNull
  public final IrValue operand;
  @NotNull
  public final Op op;

  public UnaryInstruction(@NotNull IrValue operand, @NotNull Op op) {
    super(operand.getType());
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

  @Override
  public List<? extends IrValue> getUsedValues() {
    return null;
  }
}
