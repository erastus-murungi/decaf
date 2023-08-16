package decaf.ir.codes;


import java.util.List;

import decaf.synthesis.asm.AsmWriter;
import decaf.analysis.syntax.ast.AST;
import decaf.ir.cfg.BasicBlock;
import decaf.ir.names.IrValue;
import decaf.shared.Utils;
import decaf.ir.dataflow.operand.Operand;
import decaf.ir.dataflow.operand.UnmodifiedOperand;

public class ConditionalBranch extends HasOperand implements WithTarget {

  private IrValue condition;

  private BasicBlock falseTarget;


  public ConditionalBranch(
      IrValue condition,
      BasicBlock falseTarget,
      AST source,
      String comment
  ) {
    super(
        source,
        comment
    );
    this.condition = condition;
    this.falseTarget = falseTarget;
    falseTarget.addTributary(this);
  }

  public IrValue getCondition() {
    return condition;
  }

  @Override
  public String toString() {
    return String.format(
        "%s%s %s %s %s %s %s",
        DOUBLE_INDENT,
        "if",
        condition,
        "is false goto",
        getTarget().getInstructionList()
                   .getLabel(),
        DOUBLE_INDENT + " # ",
        getComment().isPresent() ? getComment().get(): ""
    );
  }

  @Override
  public void accept(AsmWriter asmWriter) {
    asmWriter.emitInstruction(this);
  }

  @Override
  public List<IrValue> genIrValuesSurface() {
    return List.of(condition);
  }

  @Override
  public String syntaxHighlightedToString() {
    var ifString = Utils.coloredPrint(
        "if false",
        Utils.ANSIColorConstants.ANSI_GREEN_BOLD
    );
    var goTo = Utils.coloredPrint(
        "goto",
        Utils.ANSIColorConstants.ANSI_GREEN_BOLD
    );
    return String.format(
        "%s%s %s %s %s %s %s",
        DOUBLE_INDENT,
        ifString,
        condition,
        goTo,
        getTarget().getInstructionList()
                   .getLabel(),
        DOUBLE_INDENT + " # ",
        getComment().isPresent() ? getComment().get(): ""
    );
  }

  @Override
  public Instruction copy() {
    return new ConditionalBranch(condition,
                                 getTarget(),
                                 getSource(),
                                 getComment().orElse(null)
    );
  }

  @Override
  public Operand getOperand() {
    return new UnmodifiedOperand(condition);
  }

  @Override
  public List<IrValue> genOperandIrValuesSurface() {
    return List.of(condition);
  }

  public boolean replaceValue(
      IrValue oldVariable,
      IrValue replacer
  ) {
    var replaced = false;
    if (condition.equals(oldVariable)) {
      condition = replacer;
      replaced = true;
    }
    return replaced;
  }

  public BasicBlock getTarget() {
    return falseTarget;
  }

  @Override
  public void setTargetWithTributary(BasicBlock newTarget) {
    this.falseTarget = newTarget;
    newTarget.addTributary(this);

  }

  public void setFalseTarget(BasicBlock falseTarget) {
    setTargetWithTributary(falseTarget);
  }
}
