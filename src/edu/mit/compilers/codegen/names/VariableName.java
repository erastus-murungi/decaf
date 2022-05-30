package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;

public class VariableName extends AssignableName {
    public VariableName(String label, Type type) {
        super(label, type);
    }

    @Override
    public String toString() {
        return getLabel();
    }

    @Override
    public VariableName copy() {
        return new VariableName(label, type);
    }

    @Override
    public String repr() {
        return String.format("%s", getLabel());
    }
}
