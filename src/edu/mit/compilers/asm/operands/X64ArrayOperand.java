package edu.mit.compilers.asm.operands;

import org.jetbrains.annotations.NotNull;

import edu.mit.compilers.asm.X64RegisterType;

public class X64ArrayOperand extends X86Value {
    private final X86Value base;
    private final X86RegisterMappedValue index;

    public X64ArrayOperand(@NotNull X86Value base, @NotNull X86RegisterMappedValue index) {
        super(null);
        this.base = base;
        this.index = index;
    }

    @Override
    public String toString() {
        if (base instanceof X86RegisterMappedValue)
            return String.format("(%s,%s,%s)", base, index, 8);
        else {
            var stackMappedArray = (X86StackMappedValue) base;
            return String.format("-%s(%s,%s,%s)", stackMappedArray.getOffset(), X64RegisterType.RBP, index, 8);
        }
    }
}
