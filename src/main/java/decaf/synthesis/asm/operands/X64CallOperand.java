package decaf.synthesis.asm.operands;


import java.util.Collections;
import java.util.List;

import decaf.synthesis.asm.X86Register;

public class X64CallOperand extends X86Value {
  private final String methodName;
  private final boolean isImported;

  public X64CallOperand(FunctionCall functionCall) {
    super(null);
    this.methodName = functionCall.getMethodName();
    this.isImported = functionCall.isImported();
  }

  @Override
  public String toString() {
    if (isImported || methodName.equals("main"))
      return "_" + methodName;
    return methodName;
  }

  @Override
  public List<X86Register> registersInUse() {
    return Collections.emptyList();
  }
}
