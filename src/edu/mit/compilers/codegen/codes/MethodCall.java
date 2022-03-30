package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class MethodCall extends ThreeAddressCode {
    private AssignableName resultLocation;

    public MethodCall(edu.mit.compilers.ast.MethodCall methodCall, AssignableName resultLocation, String comment) {
        super(methodCall, comment);
        this.resultLocation = resultLocation;
    }

    public MethodCall(edu.mit.compilers.ast.MethodCall methodCall, String comment) {
        super(methodCall, comment);
    }

    public Optional<AssignableName> getResultLocation() {
        return Optional.ofNullable(resultLocation);
    }

    public String getMethodName() {
        return ((edu.mit.compilers.ast.MethodCall) source).nameId.id;
    }

    @Override
    public String toString() {
        if (getResultLocation().isPresent())
            return String.format("%s%s = %s %s %s%s", DOUBLE_INDENT, getResultLocation().get(), "CallMethod",getMethodName() , DOUBLE_INDENT, getComment().isPresent() ? " <<<< " + getComment().get() : "");
        return String.format("%s%s %s %s%s", DOUBLE_INDENT, "CallMethod", getMethodName(), DOUBLE_INDENT, getComment().isPresent() ? " <<<< " + getComment().get() : "");
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return resultLocation == null ? Collections.emptyList() : List.of(resultLocation);
    }
}