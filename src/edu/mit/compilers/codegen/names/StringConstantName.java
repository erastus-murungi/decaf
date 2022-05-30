package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;
import edu.mit.compilers.codegen.codes.StringLiteralAllocation;

import java.util.Objects;

public class StringConstantName extends AbstractName {
    StringLiteralAllocation stringConstant;

    public StringConstantName(StringLiteralAllocation stringConstant) {
        super(Type.String, stringConstant.label);
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
    public AbstractName copy() {
        return new StringConstantName((StringLiteralAllocation) stringConstant.copy());
    }

    @Override
    public String repr() {
        return String.format("@.%s", stringConstant.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stringConstant.stringConstant);
    }
}
