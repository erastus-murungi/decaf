package edu.mit.compilers.codegen.names;

import java.util.Objects;

import edu.mit.compilers.ast.Type;

public abstract class LValue extends Value {
    public LValue(String label, Type type) {
        super(type, label);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LValue that = (LValue) o;
        return Objects.equals(getLabel(), that.getLabel());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLabel());
    }

    public void renameForSsa(int versionNumber) {
        label = label + "." + versionNumber;
    }

    public void unRenameForSsa() {
        label = label.split("\\.")[0];
    }
}
