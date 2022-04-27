package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.BuiltinType;

public class VariableName extends AssignableName {
    public VariableName(String label, long size, BuiltinType builtinType) {
        super(label, size, builtinType);
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    public String repr() {
        return String.format("%s", label);
    }
}
