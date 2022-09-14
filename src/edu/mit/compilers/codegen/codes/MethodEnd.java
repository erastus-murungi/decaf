package edu.mit.compilers.codegen.codes;

import java.util.Collections;
import java.util.List;

import edu.mit.compilers.ast.MethodDefinition;
import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.codegen.names.IrValue;

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
    public void accept(AsmWriter asmWriter) {
        asmWriter.emitInstruction(this);
    }

    @Override
    public List<IrValue> getAllValues() {
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
