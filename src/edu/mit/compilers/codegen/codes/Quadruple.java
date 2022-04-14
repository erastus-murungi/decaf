package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A quad has four fields which we call op arg1, arg2, and result.
 * The op field contains an internal code for the operator.
 * For instance, the three address instruction x = y + z is represented by placing + in op, y in arg1, z in arg2 and x in result.
 */

public class Quadruple extends HasResult {
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
    public void swapOut(AbstractName oldName, AbstractName newName) {
        if (fstOperand.equals(oldName)) {
            fstOperand = newName;
        }
        if (sndOperand.equals(oldName)) {
            sndOperand = newName;
        }
    }

    @Override
    public Set<AbstractName> getComputationVariables() {
        return Set.of(fstOperand, sndOperand);
    }
}
