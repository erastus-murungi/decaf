package edu.mit.compilers.asm.instructions;

import org.jetbrains.annotations.NotNull;

public class X64MetaData extends X64Instruction {
    @NotNull private final String metaData;

    public X64MetaData(@NotNull String metaData) {
        this.metaData = metaData;
        verifyConstruction();
    }

    @Override
    protected void verifyConstruction() {
    }

    @Override
    public String toString() {
        return metaData;
    }
}
