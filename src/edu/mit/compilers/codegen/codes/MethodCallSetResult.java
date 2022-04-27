package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.MethodCall;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.MethodCallOperand;

import java.util.List;
import java.util.Optional;

public class MethodCallSetResult extends HasResult {
    public MethodCallSetResult(MethodCall methodCall, AssignableName resultLocation, String comment) {
        super(resultLocation, methodCall, comment);
    }

    public String getMethodName() {
        return ((MethodCall) source).nameId.id;
    }

    public String getMethodReturnType() {
        return ((MethodCall) source).builtinType.getSourceCode();
    }


    @Override
    public String toString() {
        return String.format("%s%s = %s %s %s %s%s", DOUBLE_INDENT, getResultLocation(), "call", getMethodReturnType(), getMethodName() , DOUBLE_INDENT, getComment().isPresent() ? " # " + getComment().get() : "");
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return List.of(dst);
    }

    @Override
    public String repr() {
        return String.format("%s%s: %s = %s @%s %s%s", DOUBLE_INDENT, getResultLocation().repr(), getMethodReturnType(), "call", getMethodName() , DOUBLE_INDENT, getComment().isPresent() ? " # " + getComment().get() : "");
    }

    @Override
    public Optional<Operand> getComputationNoArray() {
        if (dst instanceof ArrayName)
            return Optional.empty();
        return Optional.of(new MethodCallOperand(this));
    }
}