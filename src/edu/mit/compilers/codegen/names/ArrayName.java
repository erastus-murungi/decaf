package edu.mit.compilers.codegen.names;

public class ArrayName extends AssignableName {
    public ArrayName(String label, long size) {
        super(label, size);
    }

    @Override
    public String toString() {
        return label;
    }
}
