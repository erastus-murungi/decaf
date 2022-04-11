package edu.mit.compilers.codegen.names;

import edu.mit.compilers.codegen.TemporaryNameGenerator;

public class TemporaryName extends AssignableName {
    final long index;

    public TemporaryName(long index) {
        super();
        this.index = index;
    }

    public static TemporaryName generateTemporaryName() {
        return new TemporaryName(TemporaryNameGenerator.getNextTemporaryVariable());
    }

    @Override
    public String toString() {
        return String.format("tmp%03d", index);
    }
}
