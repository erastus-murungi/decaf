package edu.mit.compilers.asm.operands;

import static com.google.common.base.Preconditions.checkArgument;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import edu.mit.compilers.asm.X64RegisterType;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.VirtualRegister;

public class X64RegisterOperand extends X64Operand {
    @NotNull X64RegisterType x64RegisterType;

    public X64RegisterOperand(X64RegisterType x64RegisterType, @Nullable Value virtualRegister) {
        super(virtualRegister);
        checkArgument(!x64RegisterType.equals(X64RegisterType.STACK));
        this.x64RegisterType = x64RegisterType;
    }

    public static X64RegisterOperand unassigned(X64RegisterType x64RegisterType){
        return new X64RegisterOperand(x64RegisterType, null);
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
        if (!(o instanceof X64RegisterOperand that)) return false;
        return getX64RegisterType() == that.getX64RegisterType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getX64RegisterType());
    }
}
