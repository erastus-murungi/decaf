package decaf.asm.operands;


import decaf.codegen.names.IrValue;

public abstract class X86MemoryAddress extends X86Value {
  public X86MemoryAddress(IrValue irValue) {
    super(irValue);
  }

  public abstract X86MappedValue getWrapped();
}
