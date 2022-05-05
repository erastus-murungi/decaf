package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.MethodCall;

public interface FunctionCall {
    MethodCall getMethod();

    default boolean isImported() {
        return getMethod().isImported;
    }

    default int numberOfArguments() {
        return getMethod().methodCallParameterList.size();
    }

    default String getMethodName() {
        return getMethod().nameId.id;
    }

    default String getMethodReturnType() {
        return getMethod().builtinType.getSourceCode();
    }



}

