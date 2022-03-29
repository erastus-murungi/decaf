package edu.mit.compilers.codegen;

import java.util.Optional;

public class MethodCall extends ThreeAddressCode {
    private String resultLocation;
    public MethodCall(edu.mit.compilers.ast.MethodCall methodCall, String resultLocation, String comment) {
        super(methodCall, comment);
        this.resultLocation = resultLocation;
    }

    public MethodCall(edu.mit.compilers.ast.MethodCall methodCall, String comment) {
        super(methodCall, comment);
    }

    public Optional<String> getResultLocation() {
        return Optional.ofNullable(resultLocation);
    }

    @Override
    public String toString() {
        if (getResultLocation().isPresent())
            return String.format("%s%s = %s %s %s%s", DOUBLE_INDENT, getResultLocation().get(), "CallMethod", ((edu.mit.compilers.ast.MethodCall)source).nameId.id, DOUBLE_INDENT, getComment().isPresent() ? " <<<< " + getComment().get() : "");
        return String.format("%s%s %s %s%s", DOUBLE_INDENT, "CallMethod", ((edu.mit.compilers.ast.MethodCall)source).nameId.id, DOUBLE_INDENT, getComment().isPresent() ? " <<<< " + getComment().get() : "");
    }
}
