package edu.mit.compilers.codegen.names;

import java.util.Objects;

import edu.mit.compilers.ast.Type;
import edu.mit.compilers.codegen.codes.StringLiteralAllocation;

public class StringConstant extends Constant {
    StringLiteralAllocation stringConstant;

    public StringConstant(StringLiteralAllocation stringConstant) {
        super(Type.String, stringConstant.label);
        this.stringConstant = stringConstant;
    }

    @Override
    public String toString() {
        return stringConstant.label;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        StringConstant that = (StringConstant) o;
        return Objects.equals(stringConstant.stringConstant, that.stringConstant.stringConstant);
    }

    @Override
    public StringConstant copy() {
        return new StringConstant((StringLiteralAllocation) stringConstant.copy());
    }

    @Override
    public String repr() {
        return String.format("@.%s", stringConstant.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stringConstant.stringConstant);
    }

    @Override
    public void renameForSsa(int versionNumber) {
        throw new IllegalStateException();
    }

    @Override
    public String getValue() {
        return stringConstant.stringConstant;
    }
}
