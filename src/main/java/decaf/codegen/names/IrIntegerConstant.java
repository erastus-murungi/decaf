package decaf.codegen.names;


import java.util.Objects;

import decaf.ast.BooleanLiteral;
import decaf.ast.IntLiteral;
import decaf.ast.Type;

public class IrIntegerConstant extends IrConstant {
  private final Long value;

  public IrIntegerConstant(
      Long value,
      Type type
  ) {
    super(
        type,
        String.valueOf(value)
    );
    this.value = value;
  }

  public static IrIntegerConstant fromIntLiteral(IntLiteral intLiteral) {
    return new IrIntegerConstant(
        intLiteral.convertToLong(),
        Type.Int
    );
  }

  public static IrIntegerConstant fromBooleanLiteral(BooleanLiteral booleanLiteral) {
    return new IrIntegerConstant(
        booleanLiteral.convertToLong(),
        Type.Bool
    );
  }

  public static IrIntegerConstant zero() {
    return new IrIntegerConstant(
        0L,
        Type.Int
    );
  }

  public static IrIntegerConstant one() {
    return new IrIntegerConstant(
        1L,
        Type.Int
    );
  }

  public Long getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    IrIntegerConstant that = (IrIntegerConstant) o;
    return Objects.equals(
        getLabel(),
        that.getLabel()
    );
  }

  @Override
  public IrIntegerConstant copy() {
    return new IrIntegerConstant(
        Long.parseLong(getLabel()),
        type
    );
  }

  @Override
  public String toString() {
    String val = getLabel();
    if (type.equals(Type.Bool))
      val = getLabel().equals("1") ? "true": "false";
    return String.format(
        "%s",
        val
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(getLabel());
  }
}
