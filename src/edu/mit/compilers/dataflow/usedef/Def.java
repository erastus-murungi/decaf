package edu.mit.compilers.dataflow.usedef;

import edu.mit.compilers.codegen.codes.StoreInstruction;

public class Def extends UseDef {
    public Def(StoreInstruction storeInstruction) {
        super(storeInstruction.getDestination(), storeInstruction);
    }

    @Override
    public String toString() {
        return "def " + variable.repr();
    }
}
