package decaf.codegen.codes;


import java.util.List;

import decaf.asm.AsmWriter;
import decaf.codegen.names.IrMemoryAddress;
import decaf.codegen.names.IrValue;
import decaf.common.Utils;
import decaf.dataflow.operand.Operand;

public class ArrayBoundsCheck extends HasOperand {
  public GetAddress getAddress;
  public Integer boundsIndex;
  private final IrMemoryAddress destination;
  private IrValue index;
  private IrValue baseAddress;

  public ArrayBoundsCheck(
      GetAddress getAddress,
      Integer boundsIndex
  ) {
    super(null);
    this.getAddress = getAddress;
    this.boundsIndex = boundsIndex;
    this.destination = getAddress.getDestination()
                                 .copy();
    this.index = getAddress.getIndex()
                           .copy();
    this.baseAddress = getAddress.getBaseAddress()
                                 .copy();
  }

  public String getIndexIsLessThanArraySizeLabel() {
    return "index_less_than_array_length_check_done_" + boundsIndex;
  }

  public String getIndexIsNonNegativeLabel() {
    return "index_non_negative_check_done_" + boundsIndex;
  }

  @Override
  public void accept(AsmWriter asmWriter) {
    asmWriter.emitInstruction(this);
  }

  @Override
  public List<IrValue> genIrValuesSurface() {
    return getAddress.genOperandIrValuesSurface();
  }

  @Override
  public Instruction copy() {
    return new ArrayBoundsCheck(
        getAddress,
        boundsIndex
    );
  }

  @Override
  public Operand getOperand() {
    return getAddress.getOperand();
  }

  @Override
  public List<IrValue> genOperandIrValuesSurface() {
    return List.of(
        index,
        baseAddress
    );
  }

  @Override
  public boolean replaceValue(
      IrValue oldName,
      IrValue newName
  ) {
    var changesHappened = false;
    if (index == oldName) {
      index = newName;
      changesHappened = true;
    }
    if (baseAddress == oldName) {
      baseAddress = newName;
      changesHappened = true;
    }
    return changesHappened;
  }

  @Override
  public String toString() {
    return String.format(
        "%s%s %s %s, %s, %s",
        DOUBLE_INDENT,
        "checkbounds",
        destination.toString(),
        baseAddress.getType()
                   .getColoredSourceCode(),
        index,
        getAddress.getLength()
    );
  }

  public String syntaxHighlightedToString() {
    final var checkBoundsString = Utils.coloredPrint(
        "checkbounds",
        Utils.ANSIColorConstants.ANSI_GREEN_BOLD
    );
    return String.format(
        "%s%s %s %s, %s, %s",
        DOUBLE_INDENT,
        checkBoundsString,
        destination.toString(),
        baseAddress.getType()
                   .getColoredSourceCode(),
        index,
        getAddress.getLength()
    );
  }

}