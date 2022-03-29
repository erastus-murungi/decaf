package edu.mit.compilers.codegen.names;

import edu.mit.compilers.codegen.TemporaryNameGenerator;

public class TemporaryName extends AssignableName {
    final String index;

    public TemporaryName(int index) {
        this.index = String.valueOf(index);
    }

    public static TemporaryName generateTemporaryName() {
        return new TemporaryName(TemporaryNameGenerator.getNextTemporaryVariable());
    }

    @Override
    public String toString() {
        return "_t" + index;
    }
}
