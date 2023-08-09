package decaf.codegen.names;

import static decaf.common.Utils.WORD_SIZE;

import java.util.List;
import java.util.Objects;

import decaf.ast.Type;

public class IrGlobalArray extends IrValue implements IrRegisterAllocatable, IrGlobal {
  private final long numElements;

  public IrGlobalArray(
      String label,
      Type type,
      long numElements
  ) {
    super(
        type,
        label
    );
    this.numElements = numElements;
  }

  @Override
  public IrGlobalArray copy() {
    return new IrGlobalArray(
        label,
        type,
        numElements
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof IrGlobalArray irGlobalArray)) return false;
    if (!super.equals(o)) return false;
    return Objects.equals(
        getLabel(),
        irGlobalArray.getLabel()
    );
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getLabel());
  }

  @Override
  public List<IrValue> get() {
    return List.of(this);
  }

  public long getNumElements() {
    return numElements;
  }

  @Override
  public int getNumBytes() {
    return (int) getNumElements() * WORD_SIZE;
  }

  @Override
  public String toString() {
    return String.format(
        "@%s",
        getLabel()
    );
  }
}
