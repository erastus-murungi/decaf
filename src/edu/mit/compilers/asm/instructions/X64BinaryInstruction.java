package edu.mit.compilers.asm.instructions;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import edu.mit.compilers.asm.operands.X86Value;
import edu.mit.compilers.asm.types.X64BinaryInstructionType;
import edu.mit.compilers.codegen.names.IrValue;

public class X64BinaryInstruction extends X64Instruction {
    @NotNull private X64BinaryInstructionType x64BinaryInstructionType;
    @NotNull private X86Value first;
    @NotNull private X86Value second;

    public X64BinaryInstruction(@NotNull X64BinaryInstructionType x64BinaryInstructionType, @NotNull X86Value first, @NotNull X86Value second) {
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
        IrValue v = null;
        if (first.getValue() != null && second.getValue() != null) {
            return String.format("\t%s\t%s, %s\t\t# %s from %s to %s",x64BinaryInstructionType, first, second, x64BinaryInstructionType, first.getValue(), second.getValue());
        }
        if (first.getValue() != null)
            v = first.getValue();
        else if (second.getValue() != null)
            v = second.getValue();
        if (v != null)
            return String.format("\t%s\t%s, %s\t\t#%s",x64BinaryInstructionType, first, second, v);
        return String.format("\t%s\t%s, %s",x64BinaryInstructionType, first, second);
    }
}
