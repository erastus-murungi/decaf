package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.BuiltinType;

public class ArrayName extends AssignableName {
    AbstractName index;
    public ArrayName(String label, long size, BuiltinType builtinType) {
        super(label, size, builtinType);
    }

    public ArrayName(String label, long size, BuiltinType builtinType, AbstractName index) {
        super(label, size, builtinType);
        this.index = index;
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    public String repr() {
        if (index != null)
            return String.format("*%s[%s]", label, index.repr());
        return String.format("*%s", label);
    }
}
