package edu.mit.compilers.codegen.names;

import edu.mit.compilers.utils.Utils;

public class VariableName extends AssignableName {
    public VariableName(String label) {
        super(label, Utils.WORD_SIZE);
    }

    public VariableName(String label, long size) {
        super(label, size);
    }

    @Override
    public String toString() {
        return label;
    }
}
