package decaf.ir.dataflow.usedef;


import java.util.Objects;

import decaf.ir.names.IrValue;

public abstract class UseDef {
  public final IrValue variable;
  public final Instruction line;

  public UseDef(
      IrValue variable,
      Instruction line
  ) {
    this.variable = variable;
    this.line = line;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UseDef useDef = (UseDef) o;
    return Objects.equals(
        variable,
        useDef.variable
    );
  }

  @Override
  public int hashCode() {
    return variable.hashCode();
  }
}
