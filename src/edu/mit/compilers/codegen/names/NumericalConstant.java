package edu.mit.compilers.codegen.names;

import java.util.Objects;

import edu.mit.compilers.ast.BooleanLiteral;
import edu.mit.compilers.ast.Type;
import edu.mit.compilers.ast.IntLiteral;

public class NumericalConstant extends Constant {
    private final Long value;
    public NumericalConstant(Long value, Type type) {
        super(type, String.valueOf(value));
        this.value = value;
    }

    public Long getValue() {
        return value;
    }

    public static NumericalConstant fromIntLiteral(IntLiteral intLiteral) {
        return new NumericalConstant(intLiteral.convertToLong(), Type.Int);
    }

    public static NumericalConstant fromBooleanLiteral(BooleanLiteral booleanLiteral) {
        return new NumericalConstant(booleanLiteral.convertToLong(), Type.Bool);
    }

    public static NumericalConstant zero() {
        return new NumericalConstant(0L, Type.Int);
    }

    public static NumericalConstant one() {
        return new NumericalConstant(1L, Type.Int);
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
        NumericalConstant that = (NumericalConstant) o;
        return Objects.equals(getLabel(), that.getLabel());
    }

    @Override
    public void renameForSsa(int versionNumber) {
        throw new IllegalStateException();
    }

    @Override
    public NumericalConstant copy() {
        return new NumericalConstant(Long.parseLong(getLabel()), type);
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
