package decaf.asm.operands;


import java.util.List;

import decaf.asm.X86Register;
import decaf.codegen.names.IrValue;

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
