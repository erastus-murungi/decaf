package decaf.ir.codes;

import java.util.Stack;

import decaf.analysis.syntax.ast.MethodCall;
import decaf.ir.names.IrValue;

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

