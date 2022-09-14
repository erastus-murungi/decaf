package edu.mit.compilers.codegen.codes;

import java.util.Collections;
import java.util.List;

import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.codegen.names.IrValue;
import edu.mit.compilers.exceptions.DecafException;

public class RuntimeException extends Instruction {
    public final int errorCode;
    final String errorMessage;
    final DecafException decafException;

    public RuntimeException(String errorMessage, int errorCode, DecafException decafException) {
        super(null);
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.decafException = decafException;
    }

    @Override
    public void accept(AsmWriter asmWriter) {
        asmWriter.emitInstruction(this);
    }

    @Override
    public List<IrValue> getAllValues() {
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
