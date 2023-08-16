package decaf.synthesis.asm.operands;


import java.util.Collections;
import java.util.List;

import decaf.ir.names.IrConstant;
import decaf.ir.names.IrIntegerConstant;
import decaf.ir.names.IrStringConstant;
import decaf.synthesis.asm.X86Register;

public class X86ConstantValue extends X86Value {
  public X86ConstantValue(IrConstant irConstant) {
    super(irConstant);
  }

  @Override
  public String toString() {
    if (getValue() instanceof IrIntegerConstant numericalConstant) {
      return String.format(
          "$%d",
          numericalConstant.getValue()
      );
    } else if (getValue() instanceof IrStringConstant stringConstant) {
      return String.format(
          "%s(%s)",
          stringConstant.getLabel(),
          "%rip"
      );
    } else {
      throw new IllegalStateException();
    }
  }

  @Override
  public List<X86Register> registersInUse() {
    return Collections.emptyList();
  }
}
