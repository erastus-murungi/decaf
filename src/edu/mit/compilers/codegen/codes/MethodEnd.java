package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.Value;

import java.util.Collections;
import java.util.List;

public class MethodEnd extends Instruction {
    public MethodEnd(MethodDefinition methodDefinition) {
        super(methodDefinition);
    }

    public boolean isMain() {
        return methodName().equals("main");
    }

    public String methodName() {
        return ((MethodDefinition) source).methodName.getLabel();
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor , E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<Value> getAllValues() {
        return Collections.emptyList();
    }

    @Override
    public Instruction copy() {
        return new MethodEnd((MethodDefinition) source);
    }

    @Override
    public String toString() {
        return "}\n";
    }

    @Override
    public String syntaxHighlightedToString() {
        return toString();
    }

}
