package edu.mit.compilers.asm.instructions;

import org.jetbrains.annotations.NotNull;

import edu.mit.compilers.asm.operands.X64Operand;
import edu.mit.compilers.asm.types.X64UnaryInstructionType;

public class X64UnaryInstruction extends X64Instruction {
    @NotNull X64UnaryInstructionType x64UnaryInstructionType;
    @NotNull X64Operand x64Operand;

    public X64UnaryInstruction(@NotNull X64UnaryInstructionType x64UnaryInstructionType, @NotNull X64Operand x64Operand) {
        this.x64UnaryInstructionType = x64UnaryInstructionType;
        this.x64Operand = x64Operand;
        verifyConstruction();
    }

    @Override
    protected void verifyConstruction() {

    }

    @Override
    public String toString() {
        return String.format("\t%s\t%s",x64UnaryInstructionType, x64Operand);
    }
}
