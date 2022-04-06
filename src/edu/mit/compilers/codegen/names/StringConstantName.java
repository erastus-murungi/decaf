package edu.mit.compilers.codegen.names;

import edu.mit.compilers.codegen.codes.StringLiteralStackAllocation;

public class StringConstantName extends AbstractName {
    StringLiteralStackAllocation stringConstant;

    public StringConstantName(StringLiteralStackAllocation stringConstant) {
        super(8);
        this.stringConstant = stringConstant;
    }

    @Override
    public String toString() {
        return "$" + stringConstant.label;
    }
}
