package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;
import edu.mit.compilers.codegen.TemporaryNameIndexGenerator;

public class Variable extends LValue {
    boolean isTemporary = false;

    public Variable(String label, Type type, Integer versionNumber, boolean isTemporary) {
        super(label, type);
        this.versionNumber = versionNumber;
        this.isTemporary = isTemporary;
    }

    public Variable(String label, Type type, Integer versionNumber) {
        this(label, type, versionNumber, false);
    }

    public Variable(String label, Type type) {
        this(label, type, null, false);
    }

    protected Variable(long index, Type type) {
        this(String.format("%%%d", index), type, null, true);
    }

    public static Variable genTemp(Type type) {
        return new Variable(TemporaryNameIndexGenerator.getNextTemporaryVariable(), type);
    }

    @Override
    public String toString() {
        return getLabel();
    }

    @Override
    public Variable copy() {
        return new Variable(label, type, versionNumber, isTemporary);
    }

    @Override
    public String repr() {
        return String.format("%s", getLabel());
    }
}
