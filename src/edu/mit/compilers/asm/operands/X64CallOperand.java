package edu.mit.compilers.asm.operands;

import edu.mit.compilers.codegen.codes.FunctionCall;

public class X64CallOperand extends X64Operand {
    private final String methodName;
    private final boolean isImported;

    public X64CallOperand(FunctionCall functionCall) {
        this.methodName = functionCall.getMethodName();
        this.isImported = functionCall.isImported();
    }

    @Override
    public String toString() {
        if (isImported)
            return "_" + methodName;
        return methodName;
    }
}