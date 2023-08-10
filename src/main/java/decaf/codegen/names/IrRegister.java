package decaf.codegen.names;

import decaf.ast.Type;

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
