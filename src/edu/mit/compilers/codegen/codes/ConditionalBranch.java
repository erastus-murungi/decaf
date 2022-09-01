package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;
import edu.mit.compilers.utils.Utils;

import java.util.List;

public class ConditionalBranch extends HasOperand {
    public Value condition;
    public final Label falseLabel;

    public ConditionalBranch(AST source, Value condition, Label falseLabel, String comment) {
        super(source, comment);
        this.condition = condition;
        this.falseLabel = falseLabel;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s %s %s %s %s", DOUBLE_INDENT, "if", condition.repr(), "is false goto", falseLabel.getLabel(), DOUBLE_INDENT + " # ", getComment().isPresent() ? getComment().get() : "");
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<Value> getAllNames() {
        return List.of(condition);
    }

    @Override
    public String syntaxHighlightedToString() {
        var ifString =  Utils.coloredPrint("if false", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
//        var ifString =  "if false";
        var goTo =  Utils.coloredPrint("goto", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
//        var goTo =  "goto";
        return String.format("%s%s %s %s %s %s %s", DOUBLE_INDENT, ifString, condition.repr(), goTo, falseLabel.getLabel(), DOUBLE_INDENT + " # ", getComment().isPresent() ? getComment().get() : "");
    }

    @Override
    public Instruction copy() {
        return new ConditionalBranch(source, condition, falseLabel, getComment().orElse(null));
    }

    @Override
    public Operand getOperand() {
        return new UnmodifiedOperand(condition);
    }

    @Override
    public List<Value> getOperandNames() {
        return List.of(condition);
    }

    public boolean replace(Value oldVariable, Value replacer) {
        var replaced = false;
        if (condition.equals(oldVariable)) {
            condition = replacer;
            replaced = true;
        }
        return replaced;
    }

}
