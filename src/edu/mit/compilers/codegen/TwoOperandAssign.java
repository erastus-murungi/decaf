package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.symbolTable.SymbolTable;

public class TwoOperandAssign extends AbstractAssignment {
    String fstOperand;
    String operator;
    String sndOperand;


    public TwoOperandAssign(AST source, String result, String fstOperand, String operator, String sndOperand, String comment) {
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
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, SymbolTable currentSymbolTable, E extra) {
        return visitor.visit(this, currentSymbolTable, extra);
    }
}
