package decaf.asm.operands;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Objects;

import decaf.asm.X86Register;
import decaf.codegen.names.IrMemoryAddress;

public class X86MemoryAddressInRegister extends X86MemoryAddress {
  private final X86RegisterMappedValue x86RegisterMappedValue;

  public X86MemoryAddressInRegister(X86RegisterMappedValue x86RegisterMappedValue) {
    super(x86RegisterMappedValue.getValue());
    checkArgument(x86RegisterMappedValue.getValue() instanceof IrMemoryAddress);
    this.x86RegisterMappedValue = x86RegisterMappedValue;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof X86MemoryAddressInRegister that)) return false;
    return x86RegisterMappedValue.equals(that.x86RegisterMappedValue);
  }

  @Override
  public int hashCode() {
    return Objects.hash(x86RegisterMappedValue);
  }


  @Override
  public List<X86Register> registersInUse() {
    return x86RegisterMappedValue.registersInUse();
  }

  @Override
  public X86MappedValue getWrapped() {
    return x86RegisterMappedValue;
  }

  @Override
  public String toString() {
    return String.format(
        "(%s)",
        x86RegisterMappedValue
    );
  }
}
