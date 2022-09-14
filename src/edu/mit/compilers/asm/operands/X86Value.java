package edu.mit.compilers.asm.operands;


import org.jetbrains.annotations.Nullable;

import edu.mit.compilers.codegen.names.IrValue;

public abstract class X86Value {
    @Nullable
    private final IrValue irValue;

    public X86Value(@Nullable IrValue irValue) {
        this.irValue = irValue;
    }

    public @Nullable IrValue getValue() {
        return irValue;
    }
}
