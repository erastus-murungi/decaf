package edu.mit.compilers.asm.operands;

import edu.mit.compilers.asm.X64RegisterType;
import edu.mit.compilers.codegen.names.GlobalAddress;

public class X64GlobalOperand extends X64Operand {
    boolean GOT = false;
    public X64GlobalOperand(GlobalAddress globalAddress) {
        super(globalAddress);
    }

    public GlobalAddress getGlobalAddress() {
        return (GlobalAddress) getValue();
    }

    public X64GlobalOperand GOT() {
        var copy = new X64GlobalOperand(getGlobalAddress());
        copy.GOT = true;
        return copy;
    }

    @Override
    public String toString() {
        if (GOT)
            return String.format("%s@GOTPCREL(%s)", getGlobalAddress().toString(), X64RegisterType.RIP);
        return String.format("%s(%s)", getGlobalAddress().toString(), X64RegisterType.RIP);
    }
}
