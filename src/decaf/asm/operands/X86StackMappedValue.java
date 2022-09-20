package decaf.asm.operands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import decaf.asm.X86Register;
import decaf.codegen.names.IrValue;

public class X86StackMappedValue extends X86MappedValue {
    @NotNull private final X86Register baseReg;
    private final int offset;

    public int getOffset() {
        return offset;
    }

    public X86StackMappedValue(@NotNull X86Register baseReg, int offset, @Nullable IrValue irValue) {
        super(irValue);
        this.baseReg = baseReg;
        this.offset = offset;
    }

    public X86StackMappedValue(@NotNull X86Register baseReg, int offset) {
        this(baseReg, offset, null);
    }

    @Override
    public String toString() {
        if (offset == 0)
            return String.format("(%s)", baseReg);
        return String.format("%d(%s)", offset, baseReg);
    }

    @Override
    public List<X86Register> registersInUse() {
        return Collections.emptyList();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof X86StackMappedValue that)) return false;
        return getOffset() == that.getOffset() && baseReg == that.baseReg;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            baseReg,
            getOffset()
        );
    }
}
