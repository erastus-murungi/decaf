package edu.mit.compilers.codegen.names;

public class StringConstantName extends AbstractName {
    String stringConstant;

    public StringConstantName(String stringConstant) {
        this.stringConstant = stringConstant;
    }

    @Override
    public String toString() {
        return stringConstant;
    }
}
