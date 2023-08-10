package decaf.codegen.codes;


import java.util.Collections;
import java.util.List;

import decaf.asm.AsmWriter;
import decaf.codegen.names.IrValue;
import decaf.exceptions.DecafException;

public class RuntimeError extends Instruction {
  public final int errorCode;
  final String errorMessage;
  final DecafException decafException;

  public RuntimeError(
      String errorMessage,
      int errorCode,
      DecafException decafException
  ) {
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
  public List<IrValue> genIrValuesSurface() {
    return Collections.emptyList();
  }

  @Override
  public String syntaxHighlightedToString() {
    return toString();
  }

  @Override
  public Instruction copy() {
    return new RuntimeError(
        errorMessage,
        errorCode,
        decafException
    );
  }

  @Override
  public String toString() {
    return String.format(
        "%s%s(%s)",
        DOUBLE_INDENT,
        "raise RuntimeException",
        decafException.getMessage()
    );
  }
}
