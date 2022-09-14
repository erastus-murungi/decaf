package edu.mit.compilers.dataflow.usedef;

import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.IrAssignableValue;

public class Def extends UseDef {
    public Def(IrAssignableValue defined, StoreInstruction storeInstruction) {
        super(defined, storeInstruction);
    }

    @Override
    public String toString() {
        return "def " + variable;
    }
}
