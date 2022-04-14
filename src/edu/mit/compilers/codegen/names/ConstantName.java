package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.BooleanLiteral;
import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.ast.IntLiteral;

import java.util.Objects;

public class ConstantName extends AbstractName {
    String value;

    public ConstantName(Long value) {
        super();
        this.value = String.valueOf(value);
    }

    public static ConstantName fromIntLiteral(IntLiteral intLiteral) {
        return new ConstantName(intLiteral.convertToLong());
    }

    public static ConstantName fromBooleanLiteral(BooleanLiteral booleanLiteral) {
        return new ConstantName(booleanLiteral.convertToLong());
    }

    @Override
    public String toString() {
        return "$" + value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConstantName that = (ConstantName) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
