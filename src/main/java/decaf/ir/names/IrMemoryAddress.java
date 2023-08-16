package decaf.ir.names;


import java.util.List;
import java.util.Objects;

import decaf.analysis.syntax.ast.Type;

public class IrMemoryAddress extends IrRegister implements IrRegisterAllocatable {
  IrMemoryAddress(
      String label,
      Type type
  ) {
    super(
        type,
        label
    );
  }

  public IrMemoryAddress(
      long index,
      Type type
  ) {
    this(
        String.format(
            "%%%d",
            index
        ),
        type
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLabel());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof IrMemoryAddress irMemoryAddress)) return false;
    if (!super.equals(o)) return false;
    return Objects.equals(
        getLabel(),
        irMemoryAddress.getLabel()
    );
  }

  @Override
  public List<IrValue> get() {
    return List.of(this);
  }


  @Override
  public String toString() {
    return "*" + super.toString();
  }

  @Override
  public IrMemoryAddress copy() {
    return new IrMemoryAddress(
        label,
        type
    );
  }
}
