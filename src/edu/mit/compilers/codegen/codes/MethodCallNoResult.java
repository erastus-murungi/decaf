package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;

import java.util.Collections;
import java.util.List;

public class MethodCallNoResult extends ThreeAddressCode {
    public MethodCallNoResult(MethodCall methodCall, String comment) {
        super(methodCall, comment);
    }

    public boolean isImported() {
        return ((MethodCall) source).isImported;
    }

    public int numberOfArguments() {
        return ((MethodCall) source).methodCallParameterList.size();
    }

    public String getMethodName() {
        return ((MethodCall) source).nameId.id;
    }

    public String getMethodReturnType() {
        return ((MethodCall) source).builtinType.getSourceCode();
    }

    @Override
    public String toString() {
        return String.format("%s%s %s %s%s", DOUBLE_INDENT, "call", getMethodName(), DOUBLE_INDENT, getComment().isPresent() ? " #  " + getComment().get() : "");
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return Collections.emptyList();
    }

    @Override
    public String repr() {
        return String.format("%s%s %s @%s %s%s", DOUBLE_INDENT, "call", getMethodReturnType(), getMethodName(), DOUBLE_INDENT, getComment().isPresent() ? " #  " + getComment().get() : "");
    }

    @Override
    public ThreeAddressCode copy() {
        return new MethodCallNoResult((MethodCall) source, getComment().orElse(null));
    }

}