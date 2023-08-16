package decaf.ir.names;

import decaf.analysis.syntax.ast.Type;

public abstract class IrAssignable extends IrValue {
  public IrAssignable(
      Type type,
      String label
  ) {
    super(
        type,
        label
    );
  }

  public abstract IrAssignable copy();
}
