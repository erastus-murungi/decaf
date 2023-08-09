package decaf.asm.instructions;


import decaf.asm.types.X64NopInstructionType;

public class X64NoOperandInstruction extends X64Instruction {
  private final X64NopInstructionType x64NopInstructionType;

  public X64NoOperandInstruction(X64NopInstructionType x64NopInstructionType) {
    this.x64NopInstructionType = x64NopInstructionType;
    verifyConstruction();
  }

  @Override
  protected void verifyConstruction() {
  }

  @Override
  public String toString() {
    return "\t" + x64NopInstructionType;
  }
}
