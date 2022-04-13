package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;

import java.util.List;

public class Assign extends HasResult {
    public String assignmentOperator;
    public AbstractName operand;

    public Assign(AssignableName dst, String assignmentOperator, AbstractName operand, AST source, String comment) {
        super(dst, source, comment);
        this.assignmentOperator = assignmentOperator;
        this.operand = operand;
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
    public String toString() {
        return String.format("%s %s %s", dst, assignmentOperator, operand);
    }
}
