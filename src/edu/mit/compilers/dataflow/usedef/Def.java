package edu.mit.compilers.dataflow.usedef;

import edu.mit.compilers.codegen.codes.PopParameter;
import edu.mit.compilers.codegen.codes.StoreInstruction;

public class Def extends UseDef {
    public Def(StoreInstruction storeInstruction) {
        super(storeInstruction.getStore(), storeInstruction);
    }

    public Def(PopParameter popParameter) {
        super(popParameter.parameterName, popParameter);
    }


    @Override
    public String toString() {
        return "def " + variable.repr();
    }
}
