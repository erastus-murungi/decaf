package edu.mit.compilers.codegen.names;

import java.util.Objects;

import edu.mit.compilers.ast.BuiltinType;

public abstract class AssignableName extends AbstractName {
    public String label;

    public AssignableName(String label, long size, BuiltinType builtinType) {
        super(size, builtinType);
        this.label = label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AssignableName that = (AssignableName) o;
        return Objects.equals(label, that.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label);
    }
}
