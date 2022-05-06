package edu.mit.compilers.dataflow.usedef;

import edu.mit.compilers.codegen.codes.Store;
import edu.mit.compilers.dataflow.operand.Operand;

public class Def extends UseDef {
    public Operand operand;
    public Def(Store store) {
        super(store.getStore(), store);
    }


    @Override
    public String toString() {
        return "Def{\"" + variable + "\"}";
    }
}
