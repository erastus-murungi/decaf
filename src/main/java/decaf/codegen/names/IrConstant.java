package decaf.codegen.names;

import decaf.ast.Type;

public abstract class IrConstant extends IrValue {
  public IrConstant(
      Type type,
      String label
  ) {
    super(
        type,
        label
    );
  }

  public abstract Object getValue();
}
