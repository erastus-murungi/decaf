package edu.mit.compilers.codegen.codes;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.codegen.names.IrValue;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;
import edu.mit.compilers.utils.Utils;

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
        return String.format("%s%s %s %s %s %s %s", DOUBLE_INDENT, "if", condition.repr(), "is false goto", getTarget().getInstructionList().getLabel(), DOUBLE_INDENT + " # ", getComment().isPresent() ? getComment().get() : "");
    }

    @Override
    public void accept(AsmWriter asmWriter) {
        asmWriter.emitInstruction(this);
    }

    @Override
    public List<IrValue> getAllValues() {
        return List.of(condition);
    }

    @Override
    public String syntaxHighlightedToString() {
        var ifString = Utils.coloredPrint("if false", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        var goTo = Utils.coloredPrint("goto", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        return String.format("%s%s %s %s %s %s %s", DOUBLE_INDENT, ifString, condition.repr(), goTo, getTarget().getInstructionList()
                                                                                                                .getLabel(), DOUBLE_INDENT + " # ", getComment().isPresent() ? getComment().get() : "");
    }

    @Override
    public Instruction copy() {
        return new ConditionalBranch(condition, getTarget(), source, getComment().orElse(null));
    }

    @Override
    public Operand getOperand() {
        return new UnmodifiedOperand(condition);
    }

    @Override
    public List<IrValue> getOperandValues() {
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
