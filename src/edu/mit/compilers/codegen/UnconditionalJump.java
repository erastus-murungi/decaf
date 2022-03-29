package edu.mit.compilers.codegen;

import edu.mit.compilers.symbolTable.SymbolTable;

public class UnconditionalJump extends ThreeAddressCode {
    public final Label goToLabel;

    public UnconditionalJump(Label goToLabel) {
        super(null);
        this.goToLabel = goToLabel;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", DOUBLE_INDENT, "GoTo", goToLabel.label);
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, SymbolTable currentSymbolTable, E extra) {
        return visitor.visit(this, currentSymbolTable, extra);
    }
}
