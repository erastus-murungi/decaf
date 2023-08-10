package decaf.asm.instructions;


import java.util.Objects;

import decaf.asm.operands.X86Value;
import decaf.asm.types.X64BinaryInstructionType;
import decaf.codegen.names.IrValue;

public class X64BinaryInstruction extends X64Instruction {
  private final X64BinaryInstructionType x64BinaryInstructionType;
  private final X86Value first;
  private final X86Value second;

  public X64BinaryInstruction(
      X64BinaryInstructionType x64BinaryInstructionType,
      X86Value first,
      X86Value second
  ) {
    this.x64BinaryInstructionType = x64BinaryInstructionType;
    this.first = first;
    this.second = second;
    verifyConstruction();
  }

  @Override
  protected void verifyConstruction() {
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.toString()
                            .strip());
  }

  @Override
  public String noCommentToString() {
    return super.noCommentToString();
  }

  @Override
  public String toString() {
    IrValue v = null;
    if (first.getValue() != null && second.getValue() != null) {
      return String.format(
          "\t%s\t%s, %s\t\t# %s %s to %s",
          x64BinaryInstructionType,
          first,
          second,
          x64BinaryInstructionType,
          first.getValue(),
          second.getValue()
      );
    }
    if (first.getValue() != null)
      v = first.getValue();
    else if (second.getValue() != null)
      v = second.getValue();
    if (v != null)
      return String.format(
          "\t%s\t%s, %s\t\t#%s",
          x64BinaryInstructionType,
          first,
          second,
          v
      );
    return String.format(
        "\t%s\t%s, %s",
        x64BinaryInstructionType,
        first,
        second
    );
  }
}
