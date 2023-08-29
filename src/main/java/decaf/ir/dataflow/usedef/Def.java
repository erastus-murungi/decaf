package decaf.ir.dataflow.usedef;


import decaf.ir.names.IrValue;

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
