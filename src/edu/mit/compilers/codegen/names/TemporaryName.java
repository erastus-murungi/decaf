package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;
import edu.mit.compilers.codegen.TemporaryNameIndexGenerator;

public class TemporaryName extends AssignableName {
    public TemporaryName(long index, Type type) {
        super(String.format("%%%d", index), type);
    }

    public TemporaryName(String label, Type type) {
        super(label, type);
    }


    public static TemporaryName generateTemporaryName(Type type) {
        return new TemporaryName(TemporaryNameIndexGenerator.getNextTemporaryVariable(), type);
    }

    @Override
    public String toString() {
        return getLabel();
    }

    @Override
    public TemporaryName copy() {
        return new TemporaryName(label, type);
    }

    @Override
    public String repr() {
        return toString();
    }
}
