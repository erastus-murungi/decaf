package decaf.dataflow.usedef;


import decaf.codegen.codes.Instruction;
import decaf.codegen.names.IrValue;

public class Use extends UseDef {
  public Use(
      IrValue variable,
      Instruction line
  ) {
    super(
        variable,
        line
    );
  }


  @Override
  public String toString() {
    return "use " + variable;
  }
}
