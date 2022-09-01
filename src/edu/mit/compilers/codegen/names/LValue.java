package edu.mit.compilers.codegen.names;

import java.util.List;
import java.util.Objects;

import edu.mit.compilers.ast.Type;

public abstract class LValue extends Value {
    protected Integer versionNumber = null;
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
        this.versionNumber = versionNumber;
    }

    public void unRenameForSsa() {
        versionNumber = null;
    }

    public LValue copyWithIncrementedVersionNumber() {
        var copy = (LValue) copy();
        copy.versionNumber+=1;
        return copy;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public Variable newVar(String suffix) {
        return new Variable(label + suffix, this.type);
    }

    public String getLabel() {
        if (versionNumber != null)
            return String.format("%s.%d", label, versionNumber);
        return label;
    }
}
