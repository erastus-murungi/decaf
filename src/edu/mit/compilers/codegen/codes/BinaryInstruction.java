package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.MemoryAddress;
import edu.mit.compilers.dataflow.operand.BinaryOperand;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.utils.Operators;

import java.util.List;
import java.util.Optional;

/**
 * A quad has four fields which we call op arg1, arg2, and result.
 * The op field contains an internal code for the operator.
 * For instance, the three address instruction x = y + z is represented by placing + in op, y in arg1, z in arg2 and x in result.
 */

public class BinaryInstruction extends StoreInstruction {
    public Value fstOperand;
    public String operator;
    public Value sndOperand;


    public BinaryInstruction(LValue result, Value fstOperand, String operator, Value sndOperand, String comment, AST source) {
        super(result, source, comment);
        this.fstOperand = fstOperand;
        this.operator = operator;
        this.sndOperand = sndOperand;
    }

    public BinaryInstruction(LValue result, Value fstOperand, String operator, Value sndOperand) {
        this(result, fstOperand, operator, sndOperand, String.format("%s = %s %s %s", result, fstOperand, operator, sndOperand), null);
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<Value> getAllNames() {
        return List.of(getStore(), fstOperand, sndOperand);
    }

    @Override
    public Instruction copy() {
        return new BinaryInstruction(getStore(), fstOperand, operator, sndOperand, getComment().orElse(null), source);
    }

    @Override
    public Optional<Operand> getOperandNoArray() {
        if (getStore() instanceof MemoryAddress || fstOperand instanceof MemoryAddress || sndOperand instanceof MemoryAddress)
            return Optional.empty();
        return Optional.of(new BinaryOperand(this));
    }

    public boolean replace(Value oldVariable, Value replacer) {
        var replaced = false;
        if (fstOperand.equals(oldVariable)) {
            fstOperand = replacer;
            replaced = true;
        }
        if (sndOperand.equals(oldVariable)) {
            sndOperand = replacer;
            replaced = true;
        }
        return replaced;
    }

    @Override
    public Operand getOperand() {
        return new BinaryOperand(this);
    }

    @Override
    public List<Value> getOperandNames() {
        return List.of(fstOperand, sndOperand);
    }

    @Override
    public String toString() {
        if (getComment().isPresent())
            return String.format("%s%s = %s %s %s%s%s", DOUBLE_INDENT, getStore(), fstOperand, operator, sndOperand, DOUBLE_INDENT, " # " + getComment().get());
        return String.format("%s%s = %s %s %s", DOUBLE_INDENT, getStore(), fstOperand, operator, sndOperand);
    }

    public String syntaxHighlightedToString() {
        if (getComment().isPresent())
            return String.format("%s%s: %s = %s %s, %s %s%s", DOUBLE_INDENT, getStore().repr(), getStore().getType().getColoredSourceCode(), Operators.getColoredOperatorName(operator), fstOperand.repr(), sndOperand.repr(), DOUBLE_INDENT, " # " + getComment().get());
        return String.format("%s%s: %s = %s %s, %s", DOUBLE_INDENT, getStore().repr(), getStore().getType().getColoredSourceCode(), Operators.getColoredOperatorName(operator), fstOperand.repr(), sndOperand.repr());
    }

}
