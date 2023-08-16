package decaf.ir.codes;


import java.util.Collections;
import java.util.List;

import decaf.synthesis.asm.AsmWriter;
import decaf.ir.names.IrValue;

public class RuntimeError extends Instruction {
  public final int errorCode;
  final String errorMessage;

  public RuntimeError(
      String errorMessage,
      int errorCode
  ) {
    super(null);
    this.errorMessage = errorMessage;
    this.errorCode = errorCode;
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
        errorCode
    );
  }

  @Override
  public String toString() {
    return String.format(
        "%s%s",
        DOUBLE_INDENT,
        "raise RuntimeException"
    );
  }
}
