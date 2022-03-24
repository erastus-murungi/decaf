package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.MethodDefinition;

public class MethodEnd extends ThreeAddressCode {

    public MethodEnd(MethodDefinition methodDefinition) {
        super(methodDefinition);
    }

    @Override
    public String toString() {
        return "    EndFunction " + ((MethodDefinition)source).methodName.id;
    }
}
