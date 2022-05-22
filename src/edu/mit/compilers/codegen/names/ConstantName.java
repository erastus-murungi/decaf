package edu.mit.compilers.codegen.names;

import java.util.Objects;

import edu.mit.compilers.ast.BooleanLiteral;
import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.ast.IntLiteral;

public class ConstantName extends AbstractName {
    public ConstantName(Long value, BuiltinType builtinType) {
        super(builtinType, String.valueOf(value));
    }

    public static ConstantName fromIntLiteral(IntLiteral intLiteral) {
        return new ConstantName(intLiteral.convertToLong(), BuiltinType.Int);
    }

    public static ConstantName fromBooleanLiteral(BooleanLiteral booleanLiteral) {
        return new ConstantName(booleanLiteral.convertToLong(), BuiltinType.Bool);
    }

    public static ConstantName zero() {
        return new ConstantName(0L, BuiltinType.Int);
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
    public String repr() {
        String val = value;
        if (builtinType.equals(BuiltinType.Bool))
            val = value.equals("1") ? "true" : "false";
        return String.format("%s", val);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
