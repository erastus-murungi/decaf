package decaf.ir.codes;

import static decaf.shared.Utils.WORD_SIZE;

import java.util.Collections;
import java.util.List;

import decaf.synthesis.asm.AsmWriter;
import decaf.analysis.syntax.ast.AST;
import decaf.ir.names.IrGlobal;
import decaf.ir.names.IrValue;
import decaf.shared.Utils;

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
