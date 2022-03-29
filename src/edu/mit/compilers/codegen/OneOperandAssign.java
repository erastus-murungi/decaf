package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.symbolTable.SymbolTable;

public class OneOperandAssign extends AbstractAssignment {
    String operand;
    String operator;

    public OneOperandAssign(AST source, String result, String operand, String operator) {
        super(result, source);
        this.operand = operand;
        this.operator = operator;
    }

    @Override
    public String toString() {
        return String.format("%s%s = %s %s", DOUBLE_INDENT, dst, operator, operand);
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, SymbolTable currentSymbolTable, E extra) {
        return visitor.visit(this, currentSymbolTable, extra);
    }
}
