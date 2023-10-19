package decaf.ir.instructions;

import decaf.ir.IrInstructionVisitor;
import decaf.ir.types.IrType;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GetAddressInstruction extends Instruction {
  public GetAddressInstruction(@NotNull IrType type) {
    super(type);
  }


  @Override
  public String toString() {
    return null;
  }

  @Override
  protected <ArgumentType, ReturnType> ReturnType accept(@NotNull IrInstructionVisitor<ArgumentType, ReturnType> visitor, ArgumentType argument) {
    return visitor.visit(this, argument);
  }

  @Override
  public List<? extends IrValue> getUsedValues() {
    return null;
  }
}
