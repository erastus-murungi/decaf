package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.BooleanLiteral;
import edu.mit.compilers.ast.IntLiteral;

public class ConstantName extends AbstractName {
    String value;

    public ConstantName(Long value) {
        this.value = String.valueOf(value);
    }

    public static ConstantName fromIntLiteral(IntLiteral intLiteral) {
        return new ConstantName(intLiteral.convertToLong());
    }

    public static ConstantName fromBooleanLiteral(BooleanLiteral booleanLiteral) {
        return new ConstantName((long) (Boolean.parseBoolean(booleanLiteral.literal) ? 1 : 0));
    }

    @Override
    public String toString() {
        return "$" + value;
    }
}
