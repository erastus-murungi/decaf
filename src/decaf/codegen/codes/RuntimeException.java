package decaf.codegen.codes;

import java.util.Collections;
import java.util.List;

import decaf.asm.AsmWriter;
import decaf.exceptions.DecafException;
import decaf.codegen.names.IrValue;

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
