package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MethodReturn extends Instruction implements HasOperand {
    private AbstractName returnAddress;

    public MethodReturn(AST source) {
        super(source);
    }

    public MethodReturn(AST source, AbstractName returnAddress) {
        super(source);
        this.returnAddress = returnAddress;
    }

    public Optional<AbstractName> getReturnAddress() {
        return Optional.ofNullable(returnAddress);
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", DOUBLE_INDENT, "return", getReturnAddress().isEmpty() ? " " : getReturnAddress().get().repr());
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getAllNames() {
        if (returnAddress == null)
            return Collections.emptyList();
        return List.of(returnAddress);
    }

    @Override
    public String repr() {
        return toString();
    }

    @Override
    public Instruction copy() {
        return new MethodReturn(source, returnAddress);
    }

    @Override
    public Operand getOperand() {
        return new UnmodifiedOperand(returnAddress);
    }

    @Override
    public List<AbstractName> getOperandNames() {
        return List.of(returnAddress);
    }

    @Override
    public boolean replace(AbstractName oldName, AbstractName newName) {
        if (oldName.equals(returnAddress)) {
            returnAddress = newName;
            return true;
        }
        return false;
    }
}
