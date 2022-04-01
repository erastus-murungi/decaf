package edu.mit.compilers.codegen.names;

public class ArrayName extends AssignableName {
    String label;
    long length;

    public ArrayName(String label, int fieldSize, long length) {
        super(fieldSize);
        this.label = label;
        this.length = length;
    }

    public String toAsm(String indexRegister) {
        return String.format("%s(,%s,%s)", label, indexRegister, size);
    }

    public String toAsm(String indexRegister, String addressOfArray) {
        return String.format("(%s,%s,%s)", addressOfArray, indexRegister, size);
    }
}
