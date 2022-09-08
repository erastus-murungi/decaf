package edu.mit.compilers.asm.instructions;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import edu.mit.compilers.asm.operands.X64Operand;
import edu.mit.compilers.asm.types.X64BinaryInstructionType;

public class X64BinaryInstruction extends X64Instruction {
    @NotNull private X64BinaryInstructionType x64BinaryInstructionType;
    @NotNull private X64Operand first;
    @NotNull private X64Operand second;

    public X64BinaryInstruction(@NotNull X64BinaryInstructionType x64BinaryInstructionType, @NotNull X64Operand first, @NotNull X64Operand second) {
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
        return Objects.hash(this.toString().strip());
    }

    @Override
    public String noCommentToString() {
        return super.noCommentToString();
    }

    @Override
    public String toString() {
        return String.format("\t%s\t%s, %s",x64BinaryInstructionType, first, second);
    }
}
