package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.codegen.codes.ArrayAccess;

public class ArrayName extends AssignableName {
    public ArrayAccess arrayAccess;
    public ArrayName(String label, long size, BuiltinType builtinType) {
        super(label, size, builtinType);
    }

    public ArrayName(String label, long size, BuiltinType builtinType, ArrayAccess arrayAccess) {
        super(label, size, builtinType);
        this.arrayAccess = arrayAccess;
    }

    @Override
    public String toString() {
        return label;
    }

    @Override
    public String repr() {
        if (arrayAccess != null)
            return String.format("*%s[%s]", label, arrayAccess.accessIndex.repr());
        return String.format("*%s", label);
    }
}
