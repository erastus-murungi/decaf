package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.codegen.names.MemoryAddressName;
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

public class BinaryInstruction extends Store {
    public AbstractName fstOperand;
    public String operator;
    public AbstractName sndOperand;


    public BinaryInstruction(AssignableName result, AbstractName fstOperand, String operator, AbstractName sndOperand, String comment, AST source) {
        super(result, source, comment);
        this.fstOperand = fstOperand;
        this.operator = operator;
        this.sndOperand = sndOperand;
    }

    @Override
    public String toString() {
        if (getComment().isPresent())
            return String.format("%s%s = %s %s %s%s%s", DOUBLE_INDENT, store, fstOperand, operator, sndOperand, DOUBLE_INDENT, " # " + getComment().get());
        return String.format("%s%s = %s %s %s", DOUBLE_INDENT, store, fstOperand, operator, sndOperand);
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getAllNames() {
        return List.of(store, fstOperand, sndOperand);
    }

    @Override
    public String repr() {
        if (getComment().isPresent())
            return String.format("%s%s: %s = %s %s, %s %s%s", DOUBLE_INDENT, store.repr(), store.builtinType.getColoredSourceCode(), Operators.getColoredOperatorName(operator), fstOperand.repr(), sndOperand.repr(), DOUBLE_INDENT, " # " + getComment().get());
        return String.format("%s%s: %s = %s %s, %s", DOUBLE_INDENT, store.repr(), store.builtinType.getColoredSourceCode(), Operators.getColoredOperatorName(operator), fstOperand.repr(), sndOperand.repr());
    }

    @Override
    public Instruction copy() {
        return new BinaryInstruction(getStore(), fstOperand, operator, sndOperand, getComment().orElse(null), source);
    }

    @Override
    public Optional<Operand> getOperandNoArray() {
        if (store instanceof MemoryAddressName || fstOperand instanceof MemoryAddressName || sndOperand instanceof MemoryAddressName)
            return Optional.empty();
        return Optional.of(new BinaryOperand(this));
    }

    public boolean replace(AbstractName oldVariable, AbstractName replacer) {
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
    public List<AbstractName> getOperandNames() {
        return List.of(fstOperand, sndOperand);
    }

}
