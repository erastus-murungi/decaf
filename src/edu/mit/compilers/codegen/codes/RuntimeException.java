package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.exceptions.DecafException;

import java.util.Collections;
import java.util.List;

public class RuntimeException extends ThreeAddressCode {
    final String errorMessage;
    public final int errorCode;
    final DecafException decafException;

    public RuntimeException(String errorMessage, int errorCode, DecafException decafException) {
        super(null);
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.decafException = decafException;
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
        return toString();
    }

    @Override
    public String toString() {
        return String.format("%s%s(%s)", DOUBLE_INDENT, "raise RuntimeException", decafException.getMessage());
    }
}
