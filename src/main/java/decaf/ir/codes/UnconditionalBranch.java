package decaf.ir.codes;


import java.util.Collections;
import java.util.List;
import java.util.Objects;

import decaf.synthesis.asm.AsmWriter;
import decaf.ir.cfg.BasicBlock;
import decaf.ir.names.IrValue;
import decaf.shared.Utils;

public class UnconditionalBranch extends Instruction implements WithTarget {
  public BasicBlock target;

  public UnconditionalBranch(BasicBlock target) {
    super(null);
    this.target = target;
    target.addTributary(this);
  }

  public BasicBlock getTarget() {
    return target;
  }

  public void setTargetWithTributary(BasicBlock target) {
    this.target = target;
    target.addTributary(this);
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
  public String toString() {
    return String.format(
        "%s%s %s",
        DOUBLE_INDENT,
        "goto",
        getTarget().getInstructionList()
                   .getLabel()
    );
  }

  @Override
  public String syntaxHighlightedToString() {
    var goTo = Utils.coloredPrint(
        "goto",
        Utils.ANSIColorConstants.ANSI_GREEN_BOLD
    );
    return String.format(
        "%s%s %s",
        DOUBLE_INDENT,
        goTo,
        getTarget().getInstructionList()
                   .getLabel()
    );
  }

  @Override
  public Instruction copy() {
    return new UnconditionalBranch(getTarget());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UnconditionalBranch that = (UnconditionalBranch) o;
    return Objects.equals(
        getTarget().getInstructionList()
                   .getLabel(),
        that.getTarget()
            .getInstructionList()
            .getLabel()
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(getTarget().getInstructionList()
                                   .getLabel());
  }
}
