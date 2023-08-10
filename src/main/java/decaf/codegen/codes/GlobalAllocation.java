package decaf.codegen.codes;

import static decaf.common.Utils.WORD_SIZE;

import java.util.Collections;
import java.util.List;

import decaf.asm.AsmWriter;
import decaf.ast.AST;
import decaf.codegen.names.IrGlobal;
import decaf.codegen.names.IrValue;
import decaf.common.Utils;

public class GlobalAllocation extends Instruction {
  public static final int DEFAULT_ALIGNMENT = 8;
  public final int alignment;
  private final IrValue value;

  public GlobalAllocation(
      IrValue value,
      AST source,
      String comment
  ) {
    super(
        source,
        comment
    );
    this.value = value;
    this.alignment = DEFAULT_ALIGNMENT;
  }

  public IrValue getValue() {
    return value;
  }

  @Override
  public void accept(AsmWriter asmWriter) {
  }

  @Override
  public List<IrValue> genIrValuesSurface() {
    return Collections.singletonList(value);
  }

  @Override
  public String syntaxHighlightedToString() {
    var globalColoredString = Utils.coloredPrint(
        "global",
        Utils.ANSIColorConstants.ANSI_GREEN_BOLD
    );
    return String.format(
        "%s = %s %s[%s]",
        value,
        globalColoredString,
        value.getType()
             .getColoredSourceCode(),
        ((IrGlobal) getValue()).getNumBytes() / WORD_SIZE
    );
  }

  @Override
  public Instruction copy() {
    return new GlobalAllocation(value,
                                getSource(),
                                getComment().orElse(null)
    );
  }

  @Override
  public String toString() {
    var val = (IrGlobal) getValue();
    return String.format(
        "%s.comm _%s,%s,%s %s %s",
        INDENT,
        value.getLabel(),
        val.getNumBytes(),
        alignment,
        DOUBLE_INDENT,
        getComment().orElse(" ") + " " + value.getType()
                                              .getSourceCode()
    );
  }
}
