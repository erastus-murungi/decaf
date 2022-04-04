package edu.mit.compilers.codegen.names;

public class VariableName extends AssignableName {
    String label;

    public VariableName(String label, long size) {
        super(size);
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
