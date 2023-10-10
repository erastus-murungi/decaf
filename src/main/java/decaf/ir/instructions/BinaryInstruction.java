package decaf.ir.instructions;

import org.jetbrains.annotations.NotNull;

import decaf.ir.values.IrValue;

import java.util.List;

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

  public BinaryInstruction(@NotNull Op op, @NotNull IrValue lhs, @NotNull IrValue rhs, @NotNull IrValue destination) {
    super(lhs.getType());
    this.op = op;
    this.lhs = lhs;
    this.rhs = rhs;
    this.destination = destination;
  }

  @NotNull
  private final Op op;
  @NotNull
  private final IrValue lhs;
  @NotNull
  private final IrValue rhs;

  private final IrValue destination;


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

  @Override
  public List<? extends IrValue> getUsedValues() {
    return List.of(lhs, rhs, destination);
  }
}
