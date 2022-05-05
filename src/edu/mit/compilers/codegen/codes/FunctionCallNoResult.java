package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.AbstractName;

import java.util.Collections;
import java.util.List;

public class FunctionCallNoResult extends Instruction implements FunctionCall {
    public FunctionCallNoResult(MethodCall methodCall, String comment) {
        super(methodCall, comment);
    }

    @Override
    public MethodCall getMethod() {
        return (MethodCall) source;
    }
    @Override
    public String toString() {
        return String.format("%s%s %s %s%s", DOUBLE_INDENT, "call", getMethodName(), DOUBLE_INDENT, getComment().isPresent() ? " #  " + getComment().get() : "");
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getAllNames() {
        return Collections.emptyList();
    }

    @Override
    public String repr() {
        return String.format("%s%s %s @%s %s%s", DOUBLE_INDENT, "call", getMethodReturnType(), getMethodName(), DOUBLE_INDENT, getComment().isPresent() ? " #  " + getComment().get() : "");
    }

    @Override
    public Instruction copy() {
        return new FunctionCallNoResult((MethodCall) source, getComment().orElse(null));
    }

}