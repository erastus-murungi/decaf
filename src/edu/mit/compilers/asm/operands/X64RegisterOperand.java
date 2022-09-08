package edu.mit.compilers.asm.operands;

import static com.google.common.base.Preconditions.checkArgument;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import edu.mit.compilers.asm.X64RegisterType;
import edu.mit.compilers.codegen.names.LValue;

public class X64RegisterOperand extends X64Operand {
    @Nullable LValue lValue;
    @NotNull X64RegisterType x64RegisterType;

    public X64RegisterOperand(X64RegisterType x64RegisterType, @Nullable LValue lValue) {
        checkArgument(!x64RegisterType.equals(X64RegisterType.STACK));
        this.lValue = lValue;
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
}
