package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnaryOperand;

import java.util.List;
import java.util.Optional;

public class UnaryInstruction extends Store implements Cloneable, HasOperand {
    public AbstractName operand;
    public String operator;

    public UnaryInstruction(AssignableName result, String operator, AbstractName operand, AST source) {
        super(result, source);
        this.operand = operand;
        this.operator = operator;
    }

    @Override
    public String toString() {
        if (getComment().isPresent())
            return String.format("%s%s = %s %s %s %s", DOUBLE_INDENT, store, operator, operand, DOUBLE_INDENT, " # " + getComment().get());
        return String.format("%s%s = %s %s", DOUBLE_INDENT, store, operator, operand);
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getAllNames() {
        return List.of(store, operand);
    }

    @Override
    public String repr() {
        if (getComment().isPresent())
            return String.format("%s%s: %s = %s %s %s %s", DOUBLE_INDENT, store.repr(), store.builtinType.getSourceCode(), operator, operand.repr(), DOUBLE_INDENT, " # " + getComment().get());
        return String.format("%s%s: %s = %s %s", DOUBLE_INDENT, store.repr(), operator, store.builtinType.getSourceCode(), operand.repr());
    }

    @Override
    public Instruction copy() {
        return new UnaryInstruction(getStore(), operator, operand, source);
    }

    @Override
    public Optional<Operand> getOperandNoArray() {
        if (operand instanceof ArrayName || store instanceof ArrayName)
            return Optional.empty();
        return Optional.of(new UnaryOperand(this));
    }

    @Override
    public UnaryInstruction clone() {
        UnaryInstruction clone = (UnaryInstruction) super.clone();
        clone.operand = operand;
        clone.operator = operator;
        clone.setComment(getComment().orElse(null));
        clone.store = store;
        clone.source = source;
        return clone;
    }

    @Override
    public Operand getOperand() {
        return new UnaryOperand(this);
    }

    public boolean replace(AbstractName oldVariable, AbstractName replacer) {
        var replaced = false;
        if (operand.equals(oldVariable)) {
            operand = replacer;
            replaced = true;
        }
        return replaced;
    }

    @Override
    public List<AbstractName> getOperandNames() {
        return List.of(operand);
    }

}
