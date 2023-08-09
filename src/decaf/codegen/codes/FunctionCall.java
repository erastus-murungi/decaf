package decaf.codegen.codes;

import java.util.Stack;

import decaf.ast.MethodCall;
import decaf.codegen.names.IrValue;

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
    return getMethod().getType()
                      .getSourceCode();
  }

  default int getNumArguments() {
    return getArguments().size();
  }
}

