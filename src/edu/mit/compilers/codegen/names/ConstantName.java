package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.BooleanLiteral;
import edu.mit.compilers.ast.BuiltinType;
import edu.mit.compilers.ast.IntLiteral;
import edu.mit.compilers.utils.Utils;

import java.util.Objects;

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
//        var constString = Utils.coloredPrint("const", Utils.ANSIColorConstants.ANSI_PURPLE_BOLD);
        var constString = "const";
        return String.format("%s %s", constString, value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}
