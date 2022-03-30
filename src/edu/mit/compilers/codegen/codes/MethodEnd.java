package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;

import java.util.Collections;
import java.util.List;

public class MethodEnd extends ThreeAddressCode {
    public MethodEnd(MethodDefinition methodDefinition) {
        super(methodDefinition);
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", DOUBLE_INDENT, "EndFunction", ((MethodDefinition)source).methodName.id);
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor , E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return Collections.emptyList();
    }
}
