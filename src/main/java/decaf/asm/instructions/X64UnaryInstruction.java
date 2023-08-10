package decaf.asm.instructions;


import decaf.asm.operands.X86Value;
import decaf.asm.types.X64UnaryInstructionType;

public class X64UnaryInstruction extends X64Instruction {
  X64UnaryInstructionType x64UnaryInstructionType;
  X86Value x64Operand;

  public X64UnaryInstruction(
      X64UnaryInstructionType x64UnaryInstructionType,
      X86Value x64Operand
  ) {
    this.x64UnaryInstructionType = x64UnaryInstructionType;
    this.x64Operand = x64Operand;
    verifyConstruction();
  }

  @Override
  protected void verifyConstruction() {

  }

  @Override
  public String toString() {
    return String.format(
        "\t%s\t%s",
        x64UnaryInstructionType,
        x64Operand
    );
  }
}
