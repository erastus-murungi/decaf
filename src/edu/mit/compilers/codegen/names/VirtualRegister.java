package edu.mit.compilers.codegen.names;

import java.util.Objects;

import edu.mit.compilers.ast.Type;
import edu.mit.compilers.codegen.TemporaryNameIndexGenerator;

public class VirtualRegister extends LValue {
    protected Integer versionNumber;

    public VirtualRegister(String label, Type type, Integer versionNumber) {
        super(type, label);
        this.versionNumber = versionNumber;
    }

    public VirtualRegister(String label, Type type) {
        this(label, type, null);
    }

    protected VirtualRegister(long index, Type type) {
        this(String.format("%%%d", index), type, null);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLabel());
    }

    public void renameForSsa(int versionNumber) {
        this.versionNumber = versionNumber;
    }

    public void renameForSsa(VirtualRegister virtualRegister) {
        if (getType() != virtualRegister.getType())
            throw new IllegalArgumentException("type: " + getType() + "\nrename type: " + virtualRegister.getType());
        this.label = virtualRegister.label;
        this.versionNumber = virtualRegister.versionNumber;
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
    public VirtualRegister copy() {
        return new VirtualRegister(label, type, versionNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VirtualRegister virtualRegister)) return false;
        if (!super.equals(o)) return false;
        return Objects.equals(getLabel(), virtualRegister.getLabel());
    }

    @Override
    public String repr() {
        return String.format("%s", getLabel());
    }
}
