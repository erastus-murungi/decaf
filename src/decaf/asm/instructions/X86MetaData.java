package decaf.asm.instructions;

import org.jetbrains.annotations.NotNull;

public class X86MetaData extends X64Instruction {
    @NotNull private final String metaData;

    public X86MetaData(@NotNull String metaData) {
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
