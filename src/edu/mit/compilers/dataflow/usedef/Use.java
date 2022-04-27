package edu.mit.compilers.dataflow.usedef;

import edu.mit.compilers.codegen.codes.ThreeAddressCode;
import edu.mit.compilers.codegen.names.AbstractName;

public class Use extends UseDef {
    public Use(AbstractName variable, ThreeAddressCode line) {
        super(variable, line);
    }


    @Override
    public String toString() {
        return "Use{\"" + variable + "\"}";
    }
}
