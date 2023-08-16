package decaf.synthesis.asm.operands;


import java.util.Collections;
import java.util.List;

import decaf.ir.names.IrGlobalArray;
import decaf.ir.names.IrValue;
import decaf.synthesis.asm.X86Register;

public class X86GlobalValue extends X86Value {
  public X86GlobalValue(IrValue irGlobal) {
    super(irGlobal);
  }

  public IrValue getGlobalAddress() {
    return getValue();
  }

  @Override
  public String toString() {
    if (getValue() instanceof IrGlobalArray)
      return String.format(
          "_%s@GOTPCREL(%s)",
          getGlobalAddress().getLabel(),
          X86Register.RIP
      );
    return String.format(
        "%s(%s)",
        getGlobalAddress().getLabel(),
        X86Register.RIP
    );
  }

  @Override
  public List<X86Register> registersInUse() {
    return Collections.emptyList();
  }
}
