package edu.mit.compilers.asm.operands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import edu.mit.compilers.asm.X64RegisterType;
import edu.mit.compilers.codegen.names.IrAssignableValue;

public class X86StackMappedValue extends X86Value {
    @NotNull private final X64RegisterType baseReg;
    private final int offset;

    public int getOffset() {
        return offset;
    }

    public X86StackMappedValue(@NotNull X64RegisterType baseReg, int offset, @Nullable IrAssignableValue source) {
        super(source);
        this.baseReg = baseReg;
        this.offset = offset;
    }

    public X86StackMappedValue(@NotNull X64RegisterType baseReg, int offset) {
        this(baseReg, offset, null);
    }

    @Override
    public String toString() {
        if (offset == 0)
            return String.format("(%s)", baseReg);
        return String.format("%d(%s)", offset, baseReg);
    }
}
