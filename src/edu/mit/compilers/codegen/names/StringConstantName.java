package edu.mit.compilers.codegen.names;

import edu.mit.compilers.codegen.codes.StringLiteralStackAllocation;

import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        StringConstantName that = (StringConstantName) o;
        return Objects.equals(stringConstant.stringConstant, that.stringConstant.stringConstant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stringConstant.stringConstant);
    }
}
