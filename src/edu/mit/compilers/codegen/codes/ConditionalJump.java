package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;

import java.util.List;

public class ConditionalJump extends Instruction implements HasOperand {
    public AbstractName condition;
    public final Label trueLabel;

    public ConditionalJump(AST source, AbstractName condition, Label trueLabel, String comment) {
        super(source, comment);
        this.condition = condition;
        this.trueLabel = trueLabel;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s %s %s %s %s", DOUBLE_INDENT, "if", condition.repr(), "is false goto", trueLabel.label, DOUBLE_INDENT + " # ", getComment().isPresent() ? getComment().get() : "");
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getAllNames() {
        return List.of(condition);
    }

    @Override
    public String repr() {
        return toString();
    }

    @Override
    public Instruction copy() {
        return new ConditionalJump(source, condition, trueLabel, getComment().orElse(null));
    }

    @Override
    public Operand getOperand() {
        return new UnmodifiedOperand(condition);
    }

    @Override
    public List<AbstractName> getOperandNames() {
        return List.of(condition);
    }

    public boolean replace(AbstractName oldVariable, AbstractName replacer) {
        var replaced = false;
        if (condition.equals(oldVariable)) {
            condition = replacer;
            replaced = true;
        }
        return replaced;
    }

}
