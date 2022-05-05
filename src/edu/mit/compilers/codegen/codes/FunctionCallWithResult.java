package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.MethodCallOperand;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class FunctionCallWithResult extends Store implements HasOperand, FunctionCall {
    public FunctionCallWithResult(MethodCall methodCall, AssignableName resultLocation, String comment) {
        super(resultLocation, methodCall, comment);
    }
    @Override
    public MethodCall getMethod() {
        return (MethodCall) source;
    }

    @Override
    public String toString() {
        return String.format("%s%s = %s %s %s %s%s", DOUBLE_INDENT, getStore(), "call", getMethodReturnType(), getMethodName() , DOUBLE_INDENT, getComment().isPresent() ? " # " + getComment().get() : "");
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getAllNames() {
        return List.of(store);
    }

    @Override
    public String repr() {
        return String.format("%s%s: %s = %s @%s %s%s", DOUBLE_INDENT, getStore().repr(), getMethodReturnType(), "call", getMethodName() , DOUBLE_INDENT, getComment().isPresent() ? " # " + getComment().get() : "");
    }

    @Override
    public Instruction copy() {
        return new FunctionCallWithResult((MethodCall) source, getStore(), getComment().orElse(null));
    }

    @Override
    public Optional<Operand> getOperandNoArray() {
        if (store instanceof ArrayName)
            return Optional.empty();
        return Optional.of(new MethodCallOperand(this));
    }

    @Override
    public Operand getOperand() {
        return new MethodCallOperand(this);
    }

    @Override
    public List<AbstractName> getOperandNames() {
        return Collections.emptyList();
    }

    @Override
    public boolean replace(AbstractName oldName, AbstractName newName) {
        return false;
    }
}