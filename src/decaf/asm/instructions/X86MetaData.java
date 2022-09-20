package decaf.asm.instructions;

import org.jetbrains.annotations.NotNull;

public class X86MetaData extends X64Instruction {
    @NotNull private final String metaData;

    public X86MetaData(@NotNull String metaData) {
        this.metaData = metaData;
        verifyConstruction();
    }

    public static X86MetaData blockComment(@NotNull String comment) {
        return new X86MetaData(String.format("/* %s */", comment));
    }

    @Override
    protected void verifyConstruction() {
    }

    @Override
    public String toString() {
        return metaData;
    }
}
