package decaf.synthesis.asm.operands;


import decaf.ir.names.IrValue;

public abstract class X86MappedValue extends X86Value {
  public X86MappedValue(IrValue irValue) {
    super(irValue);
  }
}
