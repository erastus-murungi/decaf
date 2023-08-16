package decaf.synthesis.asm.operands;


import java.util.List;

import decaf.ir.names.IrValue;
import decaf.synthesis.asm.X86Register;

public abstract class X86Value {

  private final IrValue irValue;

  public X86Value(IrValue irValue) {
    this.irValue = irValue;
  }

  public IrValue getValue() {
    return irValue;
  }

  public abstract List<X86Register> registersInUse();
}
