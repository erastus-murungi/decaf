package edu.mit.compilers.codegen;

public class StringLiteralStackAllocation extends ThreeAddressCode {
    public String label;
    String stringConstant;

    public StringLiteralStackAllocation(String stringConstant) {
        super(null);
        label = TemporaryNameGenerator.getNextStringLiteralIndex();
        this.stringConstant = stringConstant;
    }

    public String toString() {
        return String.format("%s:\n%s%s%s", label, DOUBLE_INDENT, stringConstant, DOUBLE_INDENT + " <<<< " + stringConstant.length() + " bytes");
    }
}
