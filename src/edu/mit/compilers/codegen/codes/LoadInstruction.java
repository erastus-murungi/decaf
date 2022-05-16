package edu.mit.compilers.codegen.codes;

import java.util.List;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.AbstractName;

public class LoadInstruction extends Instruction {
    public LoadInstruction(AST source) {
        super(source);
    }

    public LoadInstruction(AST source, String comment) {
        super(source, comment);
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return null;
    }

    @Override
    public List<AbstractName> getAllNames() {
        return null;
    }

    @Override
    public String repr() {
        return null;
    }

    @Override
    public Instruction copy() {
        return null;
    }
}
