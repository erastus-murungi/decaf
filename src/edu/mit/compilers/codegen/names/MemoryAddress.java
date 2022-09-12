package edu.mit.compilers.codegen.names;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import edu.mit.compilers.ast.Type;

public class MemoryAddress extends LValue {
    private final int variableIndex;

    public MemoryAddress(@NotNull Type type, int variableIndex) {
        super(type, String.format("*%s", variableIndex));
        this.variableIndex = variableIndex;
    }

    @Override
    public MemoryAddress copy() {
        return new MemoryAddress(getType(), variableIndex);
    }

    @Override
    public String repr() {
        return getLabel();
    }

    @Override
    public String toString() {
        return getLabel();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MemoryAddress that)) return false;
        if (!super.equals(o)) return false;
        return variableIndex == that.variableIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(variableIndex);
    }
}
