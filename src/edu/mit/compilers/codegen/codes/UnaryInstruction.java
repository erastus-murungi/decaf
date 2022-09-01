package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.MemoryAddress;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnaryOperand;
import edu.mit.compilers.utils.Operators;

import java.util.List;
import java.util.Optional;

public class UnaryInstruction extends StoreInstruction {
    public Value operand;
    public String operator;

    public UnaryInstruction(LValue result, String operator, Value operand, AST source) {
        super(result, source);
        this.operand = operand;
        this.operator = operator;
    }

    @Override
    public String toString() {
        if (getComment().isPresent())
            return String.format("%s%s = %s %s %s %s", DOUBLE_INDENT, getStore(), Operators.getColoredUnaryOperatorName(operator), operand.repr(), DOUBLE_INDENT, " # " + getComment().get());
        return String.format("%s%s = %s %s", DOUBLE_INDENT, getStore(), Operators.getColoredUnaryOperatorName(operator), operand.repr());
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<Value> getAllValues() {
        return List.of(getStore(), operand);
    }

    @Override
    public String syntaxHighlightedToString() {
        return toString();
    }

    @Override
    public Instruction copy() {
        return new UnaryInstruction(getStore(), operator, operand, source);
    }

    @Override
    public Optional<Operand> getOperandNoArray() {
        if (operand instanceof MemoryAddress || getStore() instanceof MemoryAddress)
            return Optional.empty();
        return Optional.of(new UnaryOperand(this));
    }

    @Override
    public UnaryInstruction clone() {
        UnaryInstruction clone = (UnaryInstruction) super.clone();
        clone.operand = operand;
        clone.operator = operator;
        clone.setComment(getComment().orElse(null));
        clone.setStore(store);
        clone.source = source;
        return clone;
    }

    @Override
    public Operand getOperand() {
        return new UnaryOperand(this);
    }

    public boolean replace(Value oldVariable, Value replacer) {
        var replaced = false;
        if (operand.equals(oldVariable)) {
            operand = replacer;
            replaced = true;
        }
        return replaced;
    }

    @Override
    public List<Value> getOperandValues() {
        return List.of(operand);
    }

}
