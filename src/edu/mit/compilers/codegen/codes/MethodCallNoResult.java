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

    public String getMethodName() {
        return ((MethodCall) source).nameId.id;
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

}