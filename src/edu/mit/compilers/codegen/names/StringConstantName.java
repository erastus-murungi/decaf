package edu.mit.compilers.codegen.names;

public class StringConstantName extends AbstractName {
    String stringConstant;

    public StringConstantName(String stringConstant, int size) {
        super(size);
        this.stringConstant = stringConstant;
    }

    @Override
    public String toString() {
        return stringConstant;
    }
}
