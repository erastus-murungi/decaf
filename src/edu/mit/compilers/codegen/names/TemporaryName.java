package edu.mit.compilers.codegen.names;

import edu.mit.compilers.codegen.TemporaryNameGenerator;

public class TemporaryName extends AssignableName {
    final String index;

    public TemporaryName(long index, long size) {
        super(size);
        this.index = String.valueOf(index);
    }

    public static TemporaryName generateTemporaryName(long size) {
        return new TemporaryName(TemporaryNameGenerator.getNextTemporaryVariable(), size);
    }

    @Override
    public String toString() {
        return "_t" + index;
    }
}
