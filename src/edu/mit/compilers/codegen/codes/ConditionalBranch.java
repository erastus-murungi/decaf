package edu.mit.compilers.codegen.codes;

import java.util.List;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.cfg.BasicBlock;
import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;
import edu.mit.compilers.utils.Utils;

public class ConditionalBranch extends HasOperand {
    public Value condition;
    public BasicBlock falseTarget;

    public ConditionalBranch(AST source, Value condition, BasicBlock falseTarget, String comment) {
        super(source, comment);
        this.condition = condition;
        this.falseTarget = falseTarget;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s %s %s %s %s", DOUBLE_INDENT, "if", condition.repr(), "is false goto", falseTarget.getInstructionList().getLabel(), DOUBLE_INDENT + " # ", getComment().isPresent() ? getComment().get() : "");
    }

    @Override
    public void accept(AsmWriter asmWriter) {
        asmWriter.emitInstruction(this);
    }

    @Override
    public List<Value> getAllValues() {
        return List.of(condition);
    }

    @Override
    public String syntaxHighlightedToString() {
        var ifString = Utils.coloredPrint("if false", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
//        var ifString =  "if false";
        var goTo = Utils.coloredPrint("goto", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
//        var goTo =  "goto";
        return String.format("%s%s %s %s %s %s %s", DOUBLE_INDENT, ifString, condition.repr(), goTo, falseTarget.getInstructionList().getLabel(), DOUBLE_INDENT + " # ", getComment().isPresent() ? getComment().get() : "");
    }

    @Override
    public Instruction copy() {
        return new ConditionalBranch(source, condition, falseTarget, getComment().orElse(null));
    }

    @Override
    public Operand getOperand() {
        return new UnmodifiedOperand(condition);
    }

    @Override
    public List<Value> getOperandValues() {
        return List.of(condition);
    }

    public boolean replaceValue(Value oldVariable, Value replacer) {
        var replaced = false;
        if (condition.equals(oldVariable)) {
            condition = replacer;
            replaced = true;
        }
        return replaced;
    }

}
