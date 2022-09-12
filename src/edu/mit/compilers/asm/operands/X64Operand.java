package edu.mit.compilers.asm.operands;


import org.jetbrains.annotations.Nullable;

import edu.mit.compilers.codegen.names.Value;

public abstract class X64Operand {
    @Nullable
    private final Value value;

    public X64Operand(@Nullable Value value) {
        this.value = value;
    }

    public @Nullable Value getValue() {
        return value;
    }
}
