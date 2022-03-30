package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.codes.AbstractAssignment;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;

import java.util.List;

public class TwoOperandAssign extends AbstractAssignment {
    AbstractName fstOperand;
    String operator;
    AbstractName sndOperand;


    public TwoOperandAssign(AST source, AssignableName result, AbstractName fstOperand, String operator, AbstractName sndOperand, String comment) {
        super(result, source, comment);
        this.fstOperand = fstOperand;
        this.operator = operator;
        this.sndOperand = sndOperand;
    }

    @Override
    public String toString() {
        if (getComment().isPresent())
            return String.format("%s%s = %s %s %s%s%s", DOUBLE_INDENT, dst, fstOperand, operator, sndOperand, DOUBLE_INDENT, " <<<< " + getComment().get());
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
}
