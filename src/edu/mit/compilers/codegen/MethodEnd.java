package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.MethodDefinition;

public class MethodEnd extends ThreeAddressCode {

    public MethodEnd(MethodDefinition methodDefinition) {
        super(methodDefinition);
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", DOUBLE_INDENT, "EndFunction", ((MethodDefinition)source).methodName.id);
    }
}
