package edu.mit.compilers.codegen.names;

import java.util.Objects;

import edu.mit.compilers.ast.Type;

public class IrGlobal extends IrAssignableValue {
    public IrGlobal(String label, Type type) {
        super(type, label);
    }

    @Override
    public IrGlobal copy() {
        return new IrGlobal(label, type);
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
        if (!(o instanceof IrGlobal irGlobal)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getLabel(), irGlobal.getLabel());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getLabel());
    }
}
