package decaf.ir.dataflow.usedef;


import decaf.ir.names.IrValue;

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
