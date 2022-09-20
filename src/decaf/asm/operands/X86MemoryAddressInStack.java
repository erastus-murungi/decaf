package decaf.asm.operands;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import decaf.asm.X86Register;

public class X86MemoryAddressInStack extends X86MemoryAddress {
  @NotNull private X86StackMappedValue location;

  public X86MemoryAddressInStack(
      @NotNull X86StackMappedValue location
  ) {
    super(location.getValue());
    this.location = location;
  }

  @NotNull public X86StackMappedValue getLocation() {
    return location;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof X86MemoryAddressInStack that)) return false;
    return Objects.equals(
        getLocation(),
        that.getLocation()
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLocation());
  }

  @Override
  public List<X86Register> registersInUse() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull X86MappedValue getWrapped() {
    return location;
  }

  @Override
  public String toString() {
    return location.toString();
  }
}



