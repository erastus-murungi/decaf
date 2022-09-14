package edu.mit.compilers.asm.operands;

import static com.google.common.base.Preconditions.checkArgument;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import edu.mit.compilers.asm.X64RegisterType;
import edu.mit.compilers.codegen.names.IrValue;

public class X86RegisterMappedValue extends X86Value {
    @NotNull X64RegisterType x64RegisterType;

    public X86RegisterMappedValue(X64RegisterType x64RegisterType, @Nullable IrValue virtualRegister) {
        super(virtualRegister);
        checkArgument(!x64RegisterType.equals(X64RegisterType.STACK));
        this.x64RegisterType = x64RegisterType;
    }

    public static X86RegisterMappedValue unassigned(X64RegisterType x64RegisterType){
        return new X86RegisterMappedValue(x64RegisterType, null);
    }

    @Override
    public String toString() {
        return x64RegisterType.toString();
    }

    public @NotNull X64RegisterType getX64RegisterType() {
        return x64RegisterType;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof X86RegisterMappedValue that)) return false;
        return getX64RegisterType() == that.getX64RegisterType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getX64RegisterType());
    }
}
