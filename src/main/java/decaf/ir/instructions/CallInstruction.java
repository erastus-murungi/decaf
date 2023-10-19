package decaf.ir.instructions;

import decaf.ir.IrInstructionVisitor;
import decaf.ir.values.IrFunctionPointer;
import decaf.ir.values.IrRegister;
import decaf.ir.values.IrValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CallInstruction extends Instruction {
  @Nullable
  private final IrRegister result;

  @NotNull
  private final IrFunctionPointer functionPointer;

  protected CallInstruction(@NotNull IrFunctionPointer functionPointer, @Nullable IrRegister result) {
    super(functionPointer.getReturnType());
    this.result = result;
    this.functionPointer = functionPointer;
  }

    public static CallInstruction create(@NotNull IrFunctionPointer functionPointer, @Nullable IrRegister result) {
        return new CallInstruction(functionPointer, result);
    }

    public static CallInstruction createGenDest(@NotNull IrFunctionPointer functionPointer) {
        return new CallInstruction(functionPointer, IrRegister.create(functionPointer.getReturnType()));
    }

  @Override
  public String toString() {
    if (result == null) {
      return String.format("call %s",
                           functionPointer.typedPrettyPrint()
                          );
    } else {
      return String.format("%s = call %s",
                           result.prettyPrint(),
                           functionPointer.typedPrettyPrint()
                          );
    }
  }

  @Override
  protected <ArgumentType, ReturnType> ReturnType accept(@NotNull IrInstructionVisitor<ArgumentType, ReturnType> visitor, ArgumentType argument) {
    return visitor.visit(this, argument);
  }

  @Override
  public List<? extends IrValue> getUsedValues() {
    if (result == null) {
      return List.of(functionPointer);
    } else {
      return List.of(functionPointer, result);
    }
  }

    public @NotNull IrFunctionPointer getFunctionPointer() {
        return functionPointer;
    }

    public @Nullable IrRegister getDestination() {
        return result;
    }
}
