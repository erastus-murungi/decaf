package edu.mit.compilers.dataflow.usedef;

import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.dataflow.operand.Operand;

public class Def extends UseDef {
    public Operand operand;
    public Def(HasResult hasResult) {
        super(hasResult.getResultLocation(), hasResult);
    }


    @Override
    public String toString() {
        return "Def{\"" + variable + "\"}";
    }
}
