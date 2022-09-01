package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;
import edu.mit.compilers.codegen.TemporaryNameIndexGenerator;

public class Temporary extends LValue {
    public Temporary(long index, Type type) {
        super(String.format("%%%d", index), type);
    }

    public Temporary(String label, Type type) {
        super(label, type);
    }

    public static Temporary generateTemporaryName(Type type) {
        return new Temporary(TemporaryNameIndexGenerator.getNextTemporaryVariable(), type);
    }

    @Override
    public String toString() {
        return getLabel();
    }

    @Override
    public Temporary copy() {
        var temp =  new Temporary(label, type);
        temp.versionNumber = versionNumber;
        return temp;
    }

    @Override
    public String repr() {
        return toString();
    }
}
