package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.grammar.DecafScanner;

import java.util.List;
import java.util.Set;

public class Assign extends HasResult implements Cloneable {
    public String assignmentOperator;
    public AbstractName operand;

    public Assign(AssignableName dst, String assignmentOperator, AbstractName operand, AST source, String comment) {
        super(dst, source, comment);
        this.assignmentOperator = assignmentOperator;
        this.operand = operand;
    }

    public static Assign ofRegularAssign(AssignableName dst, AbstractName operand) {
        return new Assign(dst, DecafScanner.ASSIGN, operand, null, "");
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
    public void swapOut(AbstractName oldName, AbstractName newName) {
        if (operand.equals(oldName)) {
            operand = newName;
        }
    }

    @Override
    public String toString() {
        return String.format("%s%s %s %s", DOUBLE_INDENT, dst, assignmentOperator, operand);
    }

    @Override
    public Set<AbstractName> getComputationVariables() {
        return Set.of(operand);
    }

    @Override
    public Assign clone() {
        Assign clone = (Assign) super.clone();
        // TODO: copy mutable state here, so the clone can't change the internals of the original
        clone.operand = operand;
        clone.assignmentOperator = assignmentOperator;
        clone.setComment(getComment().orElse(null));
        clone.dst = dst;
        clone.source = source;
        return clone;
    }
}
