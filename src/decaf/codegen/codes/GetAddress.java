package decaf.codegen.codes;


import java.util.List;
import java.util.Objects;
import java.util.Optional;

import decaf.asm.AsmWriter;
import decaf.ast.AST;
import decaf.codegen.names.IrMemoryAddress;
import decaf.codegen.names.IrValue;
import decaf.common.Utils;
import decaf.dataflow.operand.GetAddressOperand;
import decaf.dataflow.operand.Operand;


public class GetAddress extends StoreInstruction {
  private final long length;
  private final IrValue baseAddress;
  private final IrValue index;

  public GetAddress(
      IrValue baseAddress,
      IrValue index,
      IrMemoryAddress dest,
      long length,
      AST source
  ) {
    super(
        dest,
        source
    );
    this.baseAddress = baseAddress;
    this.index = index;
    this.length = length;
  }

  public Long getLength() {
    return length;
  }

  public IrValue getIndex() {
    return index;
  }

  public IrValue getBaseAddress() {
    if (baseAddress == null)
      throw new IllegalStateException("the base address is null");
    return baseAddress;
  }

  public IrMemoryAddress getDestination() {
    return (IrMemoryAddress) super.getDestination();
  }

  @Override
  public void accept(AsmWriter visitor) {
    visitor.emitInstruction(this);
  }

  @Override
  public List<IrValue> genIrValuesSurface() {
    return List.of(
        baseAddress,
        getIndex(),
        getDestination()
    );
  }

  @Override
  public String toString() {
    return String.format(
        "%s%s: %s = %s %s : %s",
        DOUBLE_INDENT,
        getDestination(),
        baseAddress.getType()
                   .getSourceCode(),
        "getaddr",
        baseAddress,
        getIndex()
    );
  }

  @Override
  public String syntaxHighlightedToString() {
    final var getAddressString = Utils.coloredPrint(
        "getaddr",
        Utils.ANSIColorConstants.ANSI_GREEN_BOLD
    );
    return String.format(
        "%s%s: %s = %s %s : %s",
        DOUBLE_INDENT,
        getDestination(),
        baseAddress.getType()
                   .getColoredSourceCode(),
        getAddressString,
        baseAddress,
        getIndex()
    );
  }

  @Override
  public Instruction copy() {
    return new GetAddress(baseAddress,
                          index,
                          getDestination(),
                          length,
                          getSource()
    );
  }

  @Override
  public Operand getOperand() {
    return new GetAddressOperand(this);
  }

  @Override
  public List<IrValue> genOperandIrValuesSurface() {
    if (index != null)
      return List.of(
          baseAddress,
          index
      );
    return List.of(baseAddress);
  }

  @Override
  public boolean replaceValue(
      IrValue oldName,
      IrValue newName
  ) {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GetAddress that = (GetAddress) o;
    return Objects.equals(
        getBaseAddress(),
        that.getBaseAddress()
    ) && Objects.equals(
        getIndex(),
        that.getIndex()
    ) && Objects.equals(
        getLength(),
        that.getLength()
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        getBaseAddress(),
        getIndex(),
        getLength()
    );
  }

  @Override
  public Optional<Operand> getOperandNoArray() {
    return Optional.empty();
  }
}
