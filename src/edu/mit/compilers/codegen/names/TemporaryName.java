package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.codegen.TemporaryNameIndexGenerator;
import edu.mit.compilers.utils.Utils;

public class TemporaryName extends AssignableName {
    final long index;

    public TemporaryName(long index, long size, BuiltinType builtinType) {
        super(String.format("%%%d", index), size, builtinType);
        this.index = index;
    }


    public static TemporaryName generateTemporaryName(BuiltinType builtinType) {
        return new TemporaryName(TemporaryNameIndexGenerator.getNextTemporaryVariable(), Utils.WORD_SIZE, builtinType);
    }

    @Override
    public String toString() {
        return String.format("%%%d", index);
    }

    @Override
    public String repr() {
        return toString();
    }
}
