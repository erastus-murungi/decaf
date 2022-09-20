package decaf.codegen.codes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import decaf.ast.AST;
import decaf.cfg.BasicBlock;
import decaf.asm.AsmWriter;
import decaf.codegen.names.IrValue;
import decaf.dataflow.operand.Operand;
import decaf.dataflow.operand.UnmodifiedOperand;
import decaf.common.Utils;

public class ConditionalBranch extends HasOperand implements WithTarget {
    @NotNull
    private IrValue condition;
    @NotNull
    private BasicBlock falseTarget;


    public ConditionalBranch(@NotNull IrValue condition, @NotNull BasicBlock falseTarget, @Nullable AST source, @Nullable String comment) {
        super(source, comment);
        this.condition = condition;
        this.falseTarget = falseTarget;
        falseTarget.addTributary(this);
    }

    public @NotNull IrValue getCondition() {
        return condition;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s %s %s %s %s", DOUBLE_INDENT, "if", condition, "is false goto", getTarget().getInstructionList().getLabel(), DOUBLE_INDENT + " # ", getComment().isPresent() ? getComment().get() : "");
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
        var ifString = Utils.coloredPrint("if false", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        var goTo = Utils.coloredPrint("goto", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        return String.format("%s%s %s %s %s %s %s", DOUBLE_INDENT, ifString, condition, goTo, getTarget().getInstructionList()
                                                                                                                .getLabel(), DOUBLE_INDENT + " # ", getComment().isPresent() ? getComment().get() : "");
    }

    @Override
    public Instruction copy() {
        return new ConditionalBranch(condition, getTarget(),
                                     getSource(), getComment().orElse(null));
    }

    @Override
    public Operand getOperand() {
        return new UnmodifiedOperand(condition);
    }

    @Override
    public List<IrValue> genOperandIrValuesSurface() {
        return List.of(condition);
    }

    public boolean replaceValue(IrValue oldVariable, IrValue replacer) {
        var replaced = false;
        if (condition.equals(oldVariable)) {
            condition = replacer;
            replaced = true;
        }
        return replaced;
    }

    public @NotNull BasicBlock getTarget() {
        return falseTarget;
    }

    @Override
    public void setTargetWithTributary(@NotNull BasicBlock newTarget) {
        this.falseTarget = newTarget;
        newTarget.addTributary(this);

    }

    public void setFalseTarget(@NotNull BasicBlock falseTarget) {
        setTargetWithTributary(falseTarget);
    }
}
