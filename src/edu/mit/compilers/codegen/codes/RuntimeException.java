package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.exceptions.DecafException;

import java.util.Collections;
import java.util.List;

public class RuntimeException extends Instruction {
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
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<Value> getAllValues() {
        return Collections.emptyList();
    }

    @Override
    public String syntaxHighlightedToString() {
        return toString();
    }

    @Override
    public Instruction copy() {
        return new RuntimeException(errorMessage, errorCode, decafException);
    }

    @Override
    public String toString() {
        return String.format("%s%s(%s)", DOUBLE_INDENT, "raise RuntimeException", decafException.getMessage());
    }
}
