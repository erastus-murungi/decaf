package edu.mit.compilers.asm.operands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import edu.mit.compilers.asm.X64RegisterType;
import edu.mit.compilers.codegen.names.LValue;

public class X64StackOperand extends X64Operand {
    @NotNull private X64RegisterType baseReg;
    @Nullable private LValue source;
    private int offset;

    public X64StackOperand(@NotNull X64RegisterType baseReg, int offset, @Nullable LValue source) {
        this.baseReg = baseReg;
        this.offset = offset;
        this.source = source;
    }

    public X64StackOperand(@NotNull X64RegisterType baseReg, int offset) {
        this.baseReg = baseReg;
        this.offset = offset;
        this.source = null;
    }

    @Override
    public String toString() {
        if (offset == 0)
            return String.format("(%s)", baseReg);
        return String.format("%d(%s)", offset, baseReg);
    }
}
