package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.codegen.codes.ArrayAccess;
import edu.mit.compilers.utils.Utils;

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
        return value;
    }

    @Override
    public String repr() {
        String rep;
        if (arrayAccess != null) {
            rep = String.format("*%s[%s]", value, arrayAccess.accessIndex.repr());
        } else  {
        rep = String.format("*%s", value);
        }
        return Utils.coloredPrint(rep, Utils.ANSIColorConstants.ANSI_BLUE);
    }
}
