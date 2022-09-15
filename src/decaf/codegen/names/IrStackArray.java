package decaf.codegen.names;

import java.util.Objects;

import decaf.ast.Type;

public class IrStackArray extends IrValue {
  private final long numElements;

  public IrStackArray(
      Type type,
      String label,
      long numElements
  ) {
    super(type, label);
    this.numElements = numElements;
  }

  @Override
  public IrStackArray copy() {
    return new IrStackArray(type, label, numElements);
  }

  @Override
  public String toString() {
    return String.format("%s[%d]", label, numElements);
  }

  public long getNumElements() {
    return numElements;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof IrStackArray that)) return false;
    if (!super.equals(o)) return false;
    return getNumElements() == that.getNumElements() && getLabel().equals(that.getLabel());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getNumElements(), getLabel());
  }
}
