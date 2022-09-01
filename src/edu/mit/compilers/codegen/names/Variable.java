package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;

public class Variable extends LValue {
    public Variable(String label, Type type) {
        super(label, type);
    }

    public Variable(String label, Type type, Integer versionNumber) {
        super(label, type);
        this.versionNumber = versionNumber;
    }

    @Override
    public String toString() {
        return getLabel();
    }

    @Override
    public Variable copy() {
        return new Variable(label, type, versionNumber);
    }

    @Override
    public String repr() {
        return String.format("%s", getLabel());
    }
}
