package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.codegen.TemporaryNameGenerator;
import edu.mit.compilers.utils.Utils;

public class TemporaryName extends AssignableName {
    final long index;

    public TemporaryName(long index, long size, BuiltinType builtinType) {
        super(String.format("tmp%03d", index), size, builtinType);
        this.index = index;
    }

    @Override
    public String repr() {
        return Utils.coloredPrint(String.format("%%%s", index), Utils.ANSIColorConstants.ANSI_BLUE);
    }

    public static TemporaryName generateTemporaryName(BuiltinType builtinType) {
        return new TemporaryName(TemporaryNameGenerator.getNextTemporaryVariable(), Utils.WORD_SIZE, builtinType);
    }

    @Override
    public String toString() {
        return String.format("tmp%03d", index);
    }
}
