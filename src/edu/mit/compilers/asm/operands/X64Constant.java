package edu.mit.compilers.asm.operands;

import edu.mit.compilers.codegen.names.Constant;
import edu.mit.compilers.codegen.names.NumericalConstant;
import edu.mit.compilers.codegen.names.StringConstant;

public class X64Constant extends X64Operand {
    public X64Constant(Constant constant) {
        super(constant);
    }

    @Override
    public String toString() {
        if (getValue() instanceof NumericalConstant numericalConstant) {
            return String.format("$%d", numericalConstant.getValue());
        } else if (getValue() instanceof StringConstant stringConstant) {
            return String.format("%s(%s)", stringConstant, "%rip");
        } else {
            throw new IllegalStateException();
        }
    }
}
