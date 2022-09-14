package edu.mit.compilers.asm.operands;

import edu.mit.compilers.codegen.names.IrConstant;
import edu.mit.compilers.codegen.names.IrIntegerConstant;
import edu.mit.compilers.codegen.names.IrStringConstant;

public class X86ConstantValue extends X86Value {
    public X86ConstantValue(IrConstant irConstant) {
        super(irConstant);
    }

    @Override
    public String toString() {
        if (getValue() instanceof IrIntegerConstant numericalConstant) {
            return String.format("$%d", numericalConstant.getValue());
        } else if (getValue() instanceof IrStringConstant stringConstant) {
            return String.format("%s(%s)", stringConstant.getLabel(), "%rip");
        } else {
            throw new IllegalStateException();
        }
    }
}
