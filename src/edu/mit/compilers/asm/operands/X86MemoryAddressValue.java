package edu.mit.compilers.asm.operands;

import static com.google.common.base.Preconditions.checkState;
import static edu.mit.compilers.utils.Utils.WORD_SIZE;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.asm.X86Register;
import edu.mit.compilers.codegen.names.IrIntegerConstant;

public class X86MemoryAddressValue extends X86Value {
  private final X86Value base;
  private final X86Value index;

  public X86MemoryAddressValue(
      @NotNull X86Value base,
      @NotNull X86Value index
  ) {
    super(null);
      checkState(base instanceof X86RegisterMappedValue && index instanceof X86RegisterMappedValue ||
          base instanceof X86StackMappedValue &&
              (index instanceof X86ConstantValue || index instanceof X86RegisterMappedValue), String.format("base = %s, index = %s", base, index));
    this.base = base;
    this.index = index;
  }

  public X86Value getBase() {
    return base;
  }

  public X86Value getIndex() {
    return index;
  }

  @Override
  public String toString() {
    // 1) if the index is constant then the base register must be RBP
    // 2) if the base is a register then we have a global array and so the index must be a register
    // 3)

    if (base instanceof X86RegisterMappedValue) {
      return String.format("(%s,%s,%s)",
          base,
          index,
          8);
    }
    else {
      var stackMappedArray = (X86StackMappedValue) base;
      if (index instanceof X86ConstantValue x86ConstantValue) {
        checkState(x86ConstantValue.getValue() instanceof IrIntegerConstant);
        var offset = ((IrIntegerConstant) x86ConstantValue.getValue()).getValue();
        return String.format("%s(%s)",
            stackMappedArray.getOffset() + (offset * WORD_SIZE),
            X86Register.RBP);
      } else {
        checkState(index instanceof X86RegisterMappedValue);
        return String.format("%s(%s,%s,%s)",
            stackMappedArray.getOffset(),
            X86Register.RBP,
            index,
            8);
      }
    }
  }

  @Override
  public List<X86Register> registersInUse() {
    var registers = new ArrayList<X86Register>();
    if (getIndex() instanceof X86RegisterMappedValue x86RegisterMappedValue)
      registers.add(x86RegisterMappedValue.getX64RegisterType());
    if (getBase() instanceof X86RegisterMappedValue x86RegisterMappedValue)
      registers.add(x86RegisterMappedValue.getX64RegisterType());
    return registers;
  }
}
