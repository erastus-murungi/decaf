package edu.mit.compilers.dataflow.usedef;

import edu.mit.compilers.codegen.codes.PopParameter;
import edu.mit.compilers.codegen.codes.Store;
import edu.mit.compilers.dataflow.operand.Operand;

public class Def extends UseDef {
    public Def(Store store) {
        super(store.getStore(), store);
    }

    public Def(PopParameter popParameter) {
        super(popParameter.parameterName, popParameter);
    }


    @Override
    public String toString() {
        return "def " + variable.repr();
    }
}
