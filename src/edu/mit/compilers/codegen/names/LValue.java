package edu.mit.compilers.codegen.names;

import java.util.Objects;

import edu.mit.compilers.ast.Type;

public abstract class LValue extends Value {
    protected Integer versionNumber = null;

    public LValue(String label, Type type) {
        super(type, label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLabel());
    }

    public void renameForSsa(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public void renameForSsa(LValue lValue) {
        if (getType() != lValue.getType())
            throw new IllegalArgumentException();
        this.label = lValue.label;
        this.versionNumber = lValue.versionNumber;
    }

    public void clearVersionNumber() {
        versionNumber = null;
    }

    public void unRenameForSsa() {
        versionNumber = null;
    }

    public LValue copyWithIncrementedVersionNumber() {
        var copy = (LValue) copy();
        copy.versionNumber += 1;
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

    @Override
    public abstract LValue copy();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LValue lValue)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getLabel(), lValue.getLabel());
    }
}
