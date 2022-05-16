package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.utils.Utils;

public class VariableName extends AssignableName {
    public VariableName(String label, long size, BuiltinType builtinType) {
        super(label, size, builtinType);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public String repr() {
        return String.format("%s %s", builtinType.getColoredSourceCode(), value);
//        return Utils.coloredPrint(String.format("%s", value), Utils.ANSIColorConstants.ANSI_BLUE);
    }
}
