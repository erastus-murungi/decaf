package decaf.ir.codes;


import java.util.Collections;
import java.util.List;
import java.util.Optional;

import decaf.synthesis.asm.AsmWriter;
import decaf.analysis.syntax.ast.AST;
import decaf.ir.names.IrValue;
import decaf.shared.Utils;
import decaf.ir.dataflow.operand.Operand;
import decaf.ir.dataflow.operand.UnmodifiedOperand;

public class ReturnInstruction extends HasOperand {
  private IrValue returnAddress;

  public ReturnInstruction(AST source) {
    super(source);
  }

  public ReturnInstruction(
      AST source,
      IrValue returnAddress
  ) {
    super(source);
    this.returnAddress = returnAddress;
  }

  public Optional<IrValue> getReturnAddress() {
    return Optional.ofNullable(returnAddress);
  }

  @Override
  public void accept(AsmWriter asmWriter) {
    asmWriter.emitInstruction(this);
  }

  @Override
  public List<IrValue> genIrValuesSurface() {
    if (returnAddress == null)
      return Collections.emptyList();
    return List.of(returnAddress);
  }

  @Override
  public Instruction copy() {
    return new ReturnInstruction(
        getSource(),
        returnAddress
    );
  }

  @Override
  public Operand getOperand() {
    return new UnmodifiedOperand(returnAddress);
  }

  @Override
  public List<IrValue> genOperandIrValuesSurface() {
    if (returnAddress == null)
      return Collections.emptyList();
    return List.of(returnAddress);
  }

  @Override
  public boolean replaceValue(
      IrValue oldName,
      IrValue newName
  ) {
    var replaced = false;
    if (oldName.equals(returnAddress)) {
      returnAddress = newName;
      replaced = true;
    }
    return replaced;
  }

  @Override
  public String toString() {
    return String.format(
        "%s%s %s",
        DOUBLE_INDENT,
        "return",
        getReturnAddress().isEmpty() ? " ": getReturnAddress().get()
    );
  }

  @Override
  public String syntaxHighlightedToString() {
    final var returnString = Utils.coloredPrint(
        "return",
        Utils.ANSIColorConstants.ANSI_GREEN_BOLD
    );
    return String.format(
        "%s%s %s",
        DOUBLE_INDENT,
        returnString,
        getReturnAddress().isEmpty() ? " ": getReturnAddress().get()
    );
  }
}
