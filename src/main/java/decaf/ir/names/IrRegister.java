package decaf.ir.names;

import decaf.analysis.syntax.ast.Type;

public abstract class IrRegister extends IrAssignable {
  public IrRegister(
      Type type,
      String label
  ) {
    super(
        type,
        label
    );
  }
}
