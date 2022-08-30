package edu.mit.compilers.codegen.names;

import java.util.Objects;

import edu.mit.compilers.ast.BooleanLiteral;
import edu.mit.compilers.ast.Type;
import edu.mit.compilers.ast.IntLiteral;

public class ConstantName extends Value {
    public ConstantName(Long value, Type type) {
        super(type, String.valueOf(value));
    }

    public static ConstantName fromIntLiteral(IntLiteral intLiteral) {
        return new ConstantName(intLiteral.convertToLong(), Type.Int);
    }

    public static ConstantName fromBooleanLiteral(BooleanLiteral booleanLiteral) {
        return new ConstantName(booleanLiteral.convertToLong(), Type.Bool);
    }

    public static ConstantName zero() {
        return new ConstantName(0L, Type.Int);
    }

    public static ConstantName one() {
        return new ConstantName(1L, Type.Int);
    }

    @Override
    public String toString() {
        return "$" + getLabel();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ConstantName that = (ConstantName) o;
        return Objects.equals(getLabel(), that.getLabel());
    }

    @Override
    public void renameForSsa(int versionNumber) {
        throw new IllegalStateException();
    }

    @Override
    public ConstantName copy() {
        return new ConstantName(Long.parseLong(getLabel()), type);
    }

    @Override
    public String repr() {
        String val = getLabel();
        if (type.equals(Type.Bool))
            val = getLabel().equals("1") ? "true" : "false";
        return String.format("%s", val);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLabel());
    }
}
