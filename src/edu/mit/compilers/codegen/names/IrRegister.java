package edu.mit.compilers.codegen.names;

import java.util.Objects;

import edu.mit.compilers.ast.Type;
import edu.mit.compilers.codegen.TemporaryNameIndexGenerator;

public class IrRegister extends IrAssignableValue {
    protected Integer versionNumber;

    public IrRegister(String label, Type type, Integer versionNumber) {
        super(type, label);
        this.versionNumber = versionNumber;
    }

    public IrRegister(String label, Type type) {
        this(label, type, null);
    }

    protected IrRegister(long index, Type type) {
        this(String.format("%%%d", index), type, null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLabel());
    }

    public void renameForSsa(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public void renameForSsa(IrRegister irRegister) {
        if (getType() != irRegister.getType())
            throw new IllegalArgumentException("type: " + getType() + "\nrename type: " + irRegister.getType());
        this.label = irRegister.label;
        this.versionNumber = irRegister.versionNumber;
    }

    public static IrRegister gen(Type type) {
        return new IrRegister(TemporaryNameIndexGenerator.getNextTemporaryVariable(), type);
    }

    public void clearVersionNumber() {
        versionNumber = null;
    }

    public void unRenameForSsa() {
        versionNumber = null;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public String getLabel() {
        if (versionNumber != null)
            return String.format("%s.%d", label, versionNumber);
        return label;
    }

    @Override
    public String toString() {
        return getLabel();
    }

    @Override
    public IrRegister copy() {
        return new IrRegister(label, type, versionNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IrRegister irRegister)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getLabel(), irRegister.getLabel());
    }

    @Override
    public String repr() {
        return String.format("%s", getLabel());
    }
}
