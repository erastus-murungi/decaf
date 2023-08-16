package decaf.ir.codes;


import java.util.Collections;
import java.util.List;

import decaf.synthesis.asm.AsmWriter;
import decaf.ir.names.IrStringConstant;
import decaf.ir.names.IrValue;

public class StringConstantAllocation extends Instruction {
  private final IrStringConstant stringConstant;

  public StringConstantAllocation(IrStringConstant stringConstant) {
    super(null);
    this.stringConstant = stringConstant;
  }

  public IrStringConstant getStringConstant() {
    return stringConstant;
  }

  public String toString() {
    return String.format(
        "@.%s = %s",
        stringConstant.getLabel(),
        stringConstant.getValue()
    );
  }

  @Override
  public void accept(AsmWriter asmWriter) {
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
    return new StringConstantAllocation(stringConstant);
  }

  public String getASM() {
    return String.format(
        "%s:\n\t.%s   %s\n\t.align 16",
        stringConstant.getLabel(),
        "string",
        stringConstant.getValue()
    );
  }
}
