package edu.mit.compilers.codegen.names;

import java.util.Objects;

import edu.mit.compilers.ast.Type;

public class GlobalAddress extends LValue {
    public GlobalAddress(String label, Type type) {
        super(type, label);
    }

    @Override
    public GlobalAddress copy() {
        return new GlobalAddress(label, type);
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
        if (!(o instanceof GlobalAddress globalAddress)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getLabel(), globalAddress.getLabel());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getLabel());
    }
}
