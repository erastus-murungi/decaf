package edu.mit.compilers.codegen.codes;

import java.util.Stack;

import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.codegen.names.AbstractName;

public interface FunctionCall {
    MethodCall getMethod();

    default boolean isImported() {
        return getMethod().isImported;
    }

    default int numberOfArguments() {
        return getMethod().methodCallParameterList.size();
    }

    default String getMethodName() {
        return getMethod().nameId.getLabel();
    }

    default String getMethodReturnType() {
        return getMethod().getType().getSourceCode();
    }

    public Stack<AbstractName> getArguments();
}

