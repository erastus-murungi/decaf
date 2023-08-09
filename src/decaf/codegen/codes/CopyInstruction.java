package decaf.codegen.codes;


import java.util.List;
import java.util.Optional;

import decaf.asm.AsmWriter;
import decaf.ast.AST;
import decaf.codegen.names.IrAssignable;
import decaf.codegen.names.IrMemoryAddress;
import decaf.codegen.names.IrValue;
import decaf.dataflow.operand.Operand;
import decaf.dataflow.operand.UnmodifiedOperand;

public class CopyInstruction extends StoreInstruction implements Cloneable {
  private IrValue irValue;

  public CopyInstruction(
      IrAssignable dst,
      IrValue operand,
      AST source,
      String comment
  ) {
    super(
        dst,
        source,
        comment
    );
    this.irValue = operand;
  }

  public static CopyInstruction noAstConstructor(
      IrAssignable dst,
      IrValue operand
  ) {
    return new CopyInstruction(
        dst,
        operand,
        null,
        String.format("%s = %s",
                      dst,
                      operand
        )
    );
  }

  public static CopyInstruction noMetaData(
      IrAssignable dst,
      IrValue operand
  ) {
    return new CopyInstruction(
        dst,
        operand,
        null,
        ""
    );
  }

  public IrValue getValue() {
    return irValue;
  }

  @Override
  public void accept(AsmWriter asmWriter) {
    asmWriter.emitInstruction(this);
  }

  @Override
  public List<IrValue> genIrValuesSurface() {
    return List.of(
        getDestination(),
        irValue
    );
  }

  @Override
  public Instruction copy() {
    return new CopyInstruction(getDestination(),
                               irValue,
                               getSource(),
                               getComment().orElse(null)
    );
  }

  @Override
  public Optional<Operand> getOperandNoArray() {
    if (irValue instanceof IrMemoryAddress)
      return Optional.empty();
    return Optional.of(new UnmodifiedOperand(irValue));
  }

  public boolean contains(IrValue name) {
    return getDestination().equals(name) || irValue.equals(name);
  }

  @Override
  public CopyInstruction clone() {
    CopyInstruction clone = (CopyInstruction) super.clone();
    // TODO: copy mutable state here, so the clone can't change the internals of the original
    clone.irValue = irValue;
    clone.setComment(getComment().orElse(null));
    clone.setDestination(getDestination());
    clone.setSource(getSource());
    return clone;
  }

  @Override
  public Operand getOperand() {
    return new UnmodifiedOperand(irValue);
  }

  @Override
  public List<IrValue> genOperandIrValuesSurface() {
    return List.of(irValue);
  }

  public boolean replaceValue(
      IrValue oldVariable,
      IrValue replacer
  ) {
    var replaced = false;
    if (irValue.equals(oldVariable)) {
      irValue = replacer;
      replaced = true;
    }
    return replaced;
  }

  @Override
  public String toString() {
    return String.format(
        "%s%s %s: %s = %s",
        DOUBLE_INDENT,
        getPrefix(),
        getDestination(),
        getDestination().getType()
                        .getSourceCode(),
        irValue
    );
  }

  @Override
  public String syntaxHighlightedToString() {
    return String.format(
        "%s%s %s: %s = %s",
        DOUBLE_INDENT,
        getPrefixSyntaxHighlighted(),
        getDestination(),
        getDestination().getType()
                        .getColoredSourceCode(),
        irValue
    );
  }

}
