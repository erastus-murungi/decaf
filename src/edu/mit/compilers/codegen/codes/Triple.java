package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.ThreeAddressCodeVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.grammar.DecafScanner;

import java.util.List;

public class Triple extends Assignment {
    public AbstractName operand;
    public String operator;

    public Triple(AssignableName result, String operator, AbstractName operand, AST source) {
        super(result, source);
        this.operand = operand;
        this.operator = operator;
    }

    @Override
    public String toString() {
        if (operator.equals(DecafScanner.ASSIGN))
            return String.format("%s%s %s %s %s # %s", DOUBLE_INDENT, dst, operator, operand, DOUBLE_INDENT, getComment().orElse(""));
        return String.format("%s%s = %s %s %s # %s", DOUBLE_INDENT, dst, operator, operand, DOUBLE_INDENT, getComment().orElse(""));
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getNames() {
        return List.of(dst, operand);
    }
}
