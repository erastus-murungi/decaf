package decaf.asm.operands;

import static com.google.common.base.Preconditions.checkArgument;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import decaf.asm.X86Register;
import decaf.codegen.names.IrValue;

public class X86RegisterMappedValue extends X86MappedValue {
    @NotNull X86Register x86Register;

    public X86RegisterMappedValue(X86Register x86Register, @Nullable IrValue virtualRegister) {
        super(virtualRegister);
        checkArgument(!x86Register.equals(X86Register.STACK));
        this.x86Register = x86Register;
    }

    public static X86RegisterMappedValue unassigned(X86Register x86Register){
        return new X86RegisterMappedValue(x86Register, null);
    }

    @Override
    public String toString() {
        return x86Register.toString();
    }

    public @NotNull X86Register getX64RegisterType() {
        return x86Register;
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

    @Override
    public List<X86Register> registersInUse() {
        return List.of(x86Register);
    }
}
