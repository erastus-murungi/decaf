package decaf.asm.operands;


import decaf.codegen.names.IrValue;

public abstract class X86MappedValue extends X86Value {
  public X86MappedValue(IrValue irValue) {
    super(irValue);
  }
}
