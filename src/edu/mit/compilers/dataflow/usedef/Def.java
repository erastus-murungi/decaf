package edu.mit.compilers.dataflow.usedef;

import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.codegen.names.IrValue;

public class Def extends UseDef {
    public Def(IrValue defined, StoreInstruction storeInstruction) {
        super(defined, storeInstruction);
    }

    @Override
    public String toString() {
        return "def " + variable;
    }
}
