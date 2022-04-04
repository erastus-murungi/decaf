package edu.mit.compilers.codegen.names;

import java.util.Objects;

public class ArrayName extends AssignableName {
    public VariableName label;
    public long length;
    public AbstractName index;

    public ArrayName(String label, int fieldSize, long length, AbstractName index) {
        super(fieldSize);
        this.label = new VariableName(label, fieldSize);
        this.length = length;
        this.index = index;
    }

    @Override
    public String toString() {
        return String.format("%s[%s]", label, index);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArrayName arrayName = (ArrayName) o;
        return Objects.equals(label, arrayName.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), label, length, index);
    }
}
