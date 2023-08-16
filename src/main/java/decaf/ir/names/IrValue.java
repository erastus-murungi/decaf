package decaf.ir.names;


import java.util.UUID;

import decaf.analysis.syntax.ast.Type;

public abstract class IrValue {
  protected final UUID uuid;
  protected Type type;
  protected String label;

  public IrValue(
      Type type,
      String label
  ) {
    this.type = type;
    this.label = label;
    uuid = UUID.randomUUID();
  }

  public UUID getUuid() {
    return uuid;
  }

  public Type getType() {
    return type;
  }

  public String getLabel() {
    return label;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IrValue that = (IrValue) o;
    return toString().equals(that.toString());
  }

  public abstract IrValue copy();

  @Override
  public String toString() {
    return getLabel();
  }

  public boolean isGlobal() {
    return false;
  }
}
