package edu.mit.compilers.asm.operands;

import edu.mit.compilers.asm.X64RegisterType;

public class X64ArrayOperand extends X64Operand {
    private final X64RegisterOperand base;
    private final X64RegisterOperand index;
    private final int offset;

    public X64ArrayOperand(X64RegisterOperand base, X64RegisterOperand index, int offset) {
        super(null);
        this.base = base;
        this.index = index;
        this.offset = offset;
    }

    @Override
    public String toString() {
        if (offset == 0)
            return String.format("(%s,%s,%s)", base, index, 8);
        else
            return String.format("-%s(%s,%s,%s)", offset, X64RegisterType.RBP, index, 8);
    }
}
