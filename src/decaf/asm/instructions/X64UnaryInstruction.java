package decaf.asm.instructions;

import org.jetbrains.annotations.NotNull;

import decaf.asm.operands.X86Value;
import decaf.asm.types.X64UnaryInstructionType;

public class X64UnaryInstruction extends X64Instruction {
    @NotNull X64UnaryInstructionType x64UnaryInstructionType;
    @NotNull X86Value x64Operand;

    public X64UnaryInstruction(@NotNull X64UnaryInstructionType x64UnaryInstructionType, @NotNull X86Value x64Operand) {
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
