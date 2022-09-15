package decaf.asm.operands;


import org.jetbrains.annotations.Nullable;

import java.util.List;

import decaf.asm.X86Register;
import decaf.codegen.names.IrValue;

public abstract class X86Value {
    @Nullable
    private final IrValue irValue;

    public X86Value(@Nullable IrValue irValue) {
        this.irValue = irValue;
    }

    public @Nullable IrValue getValue() {
        return irValue;
    }

    public abstract List<X86Register> registersInUse();
}
