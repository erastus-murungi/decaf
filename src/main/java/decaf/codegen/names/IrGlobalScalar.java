package decaf.codegen.names;

import static decaf.common.Utils.WORD_SIZE;

import java.util.Objects;

import decaf.ast.Type;

public class IrGlobalScalar extends IrAssignable implements IrGlobal {
  public IrGlobalScalar(
      String label,
      Type type
  ) {
    super(
        type,
        label
    );
  }

  @Override
  public IrGlobalScalar copy() {
    return new IrGlobalScalar(
        label,
        type
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof IrGlobalScalar irGlobalScalar)) return false;
    if (!super.equals(o)) return false;
    return Objects.equals(
        getLabel(),
        irGlobalScalar.getLabel()
    );
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getLabel());
  }

  @Override
  public int getNumBytes() {
    return WORD_SIZE;
  }

  @Override
  public String toString() {
    return String.format(
        "@%s",
        getLabel()
    );
  }
}
