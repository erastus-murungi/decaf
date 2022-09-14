package edu.mit.compilers.codegen.names;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import edu.mit.compilers.ast.Type;

public class IrMemoryAddress extends IrAssignableValue {
    private final int variableIndex;
    private final IrAssignableValue baseAddress;
    private final IrValue index;

    public IrMemoryAddress(@NotNull Type type, int variableIndex, @NotNull IrAssignableValue baseAddress, @NotNull IrValue index) {
        super(type, String.format("*%s", variableIndex));
        this.variableIndex = variableIndex;
        this.baseAddress = baseAddress;
        this.index = index;
    }

    @Override
    public IrMemoryAddress copy() {
        return new IrMemoryAddress(getType(), variableIndex, baseAddress, index);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IrMemoryAddress that)) return false;
        if (!super.equals(o)) return false;
        return variableIndex == that.variableIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableIndex);
    }

    public IrAssignableValue getBaseAddress() {
        return baseAddress;
    }

    public IrValue getIndex() {
        return index;
    }
}
