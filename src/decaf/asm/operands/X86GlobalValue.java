package decaf.asm.operands;

import java.util.Collections;
import java.util.List;

import decaf.codegen.names.IrGlobalArray;
import decaf.asm.X86Register;
import decaf.codegen.names.IrGlobal;

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
            return String.format("%s@GOTPCREL(%s)", getGlobalAddress().toString(), X86Register.RIP);
        return String.format("%s(%s)", getGlobalAddress().toString(), X86Register.RIP);
    }

    @Override
    public List<X86Register> registersInUse() {
        return Collections.emptyList();
    }
}
