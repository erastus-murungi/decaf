package decaf.asm.operands;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import decaf.codegen.names.IrValue;

public abstract class X86MemoryAddress extends X86Value {
  public X86MemoryAddress(@Nullable IrValue irValue) {
    super(irValue);
  }

  @NotNull public abstract X86MappedValue getWrapped();
}
