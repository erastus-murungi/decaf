package edu.mit.compilers.codegen.names;

import java.util.Objects;

public abstract class AssignableName extends AbstractName {
    public String label;

    public AssignableName() {
        super();
    }

    public AssignableName(String label, long size) {
        super(size);
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
