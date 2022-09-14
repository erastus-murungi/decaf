package edu.mit.compilers.asm.operands;

import edu.mit.compilers.asm.X64RegisterType;
import edu.mit.compilers.codegen.names.IrGlobal;
import edu.mit.compilers.codegen.names.IrGlobalArray;

public class X86GlobalValue extends X86Value {
    public X86GlobalValue(IrGlobal irGlobal) {
        super(irGlobal);
    }

    public IrGlobal getGlobalAddress() {
        return (IrGlobal) getValue();
    }

    @Override
    public String toString() {
        if (getValue() instanceof IrGlobalArray)
            return String.format("%s@GOTPCREL(%s)", getGlobalAddress().toString(), X64RegisterType.RIP);
        return String.format("%s(%s)", getGlobalAddress().toString(), X64RegisterType.RIP);
    }
}
