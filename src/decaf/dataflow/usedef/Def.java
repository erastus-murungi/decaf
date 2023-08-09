package decaf.dataflow.usedef;


import decaf.codegen.codes.StoreInstruction;
import decaf.codegen.names.IrValue;

public class Def extends UseDef {
  public Def(
      IrValue defined,
      StoreInstruction storeInstruction
  ) {
    super(
        defined,
        storeInstruction
    );
  }

  @Override
  public String toString() {
    return "def " + variable;
  }
}
