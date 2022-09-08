package edu.mit.compilers.asm.instructions;

import org.jetbrains.annotations.NotNull;

import edu.mit.compilers.asm.types.X64NopInstructionType;

public class X64NoOperandInstruction extends X64Instruction {
    @NotNull private final X64NopInstructionType x64NopInstructionType;

    public X64NoOperandInstruction(@NotNull X64NopInstructionType x64NopInstructionType) {
        this.x64NopInstructionType = x64NopInstructionType;
        verifyConstruction();
    }

    @Override
    protected void verifyConstruction() {}

    @Override
    public String toString() {
        return "\t" + x64NopInstructionType;
    }
}
