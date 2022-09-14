package edu.mit.compilers.codegen.codes;

import java.util.Stack;

import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.codegen.names.IrValue;

public interface FunctionCall {
    MethodCall getMethod();

    Stack<IrValue> getArguments();

    default boolean isImported() {
        return getMethod().isImported;
    }

    default String getMethodName() {
        return getMethod().nameId.getLabel();
    }

    default String getMethodReturnType() {
        return getMethod().getType().getSourceCode();
    }

    default int getNumArguments() {
        return getArguments().size();
    }
}

