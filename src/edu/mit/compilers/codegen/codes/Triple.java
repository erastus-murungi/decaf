package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.ArrayName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnaryOperand;

import java.util.List;
import java.util.Optional;

public class Triple extends HasResult implements Cloneable, HasOperand {
    public AbstractName operand;
    public String operator;

    public Triple(AssignableName result, String operator, AbstractName operand, AST source) {
        super(result, source);
        this.operand = operand;
        this.operator = operator;
    }

    @Override
    public String toString() {
        if (getComment().isPresent())
            return String.format("%s%s = %s %s %s %s", DOUBLE_INDENT, dst, operator, operand, DOUBLE_INDENT, " # " + getComment().get());
        return String.format("%s%s = %s %s", DOUBLE_INDENT, dst, operator, operand);
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return List.of(dst, operand);
    }

    @Override
    public Optional<Operand> getComputationNoArray() {
        if (operand instanceof ArrayName || dst instanceof ArrayName)
            return Optional.empty();
        return Optional.of(new UnaryOperand(this));
    }

    @Override
    public Triple clone() {
        Triple clone = (Triple) super.clone();
        // TODO: copy mutable state here, so the clone can't change the internals of the original
        clone.operand = operand;
        clone.operator = operator;
        clone.setComment(getComment().orElse(null));
        clone.dst = dst;
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
    public boolean hasUnModifiedOperand() {
        return false;
    }

    @Override
    public List<AbstractName> getOperandNames() {
        return List.of(operand);
    }

}
