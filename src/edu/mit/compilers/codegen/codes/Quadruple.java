package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.dataflow.operand.BinaryOperand;
import edu.mit.compilers.dataflow.operand.Operand;

import java.util.List;
import java.util.Optional;

/**
 * A quad has four fields which we call op arg1, arg2, and result.
 * The op field contains an internal code for the operator.
 * For instance, the three address instruction x = y + z is represented by placing + in op, y in arg1, z in arg2 and x in result.
 */

public class Quadruple extends HasResult implements HasOperand {
    public AbstractName fstOperand;
    public String operator;
    public AbstractName sndOperand;


    public Quadruple(AssignableName result, AbstractName fstOperand, String operator, AbstractName sndOperand, String comment, AST source) {
        super(result, source, comment);
        this.fstOperand = fstOperand;
        this.operator = operator;
        this.sndOperand = sndOperand;
    }

    @Override
    public String toString() {
        if (getComment().isPresent())
            return String.format("%s%s = %s %s %s%s%s", DOUBLE_INDENT, dst, fstOperand, operator, sndOperand, DOUBLE_INDENT, " # " + getComment().get());
        return String.format("%s%s = %s %s %s", DOUBLE_INDENT, dst, fstOperand, operator, sndOperand);
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return List.of(dst, fstOperand, sndOperand);
    }

    @Override
    public String repr() {
        if (getComment().isPresent())
            return String.format("%s%s: %s = %s %s %s %s%s", DOUBLE_INDENT, dst.repr(), dst.builtinType.getSourceCode(), fstOperand.repr(), operator, sndOperand.repr(), DOUBLE_INDENT, " # " + getComment().get());
        return String.format("%s%s: %s = %s %s %s", DOUBLE_INDENT, dst.repr(), dst.builtinType.getSourceCode(), fstOperand.repr(), operator, sndOperand.repr());
    }

    @Override
    public Optional<Operand> getComputationNoArray() {
        if (dst instanceof ArrayName || fstOperand instanceof ArrayName || sndOperand instanceof ArrayName)
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
    public boolean hasUnModifiedOperand() {
        return false;
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
