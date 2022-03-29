package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.symbolTable.SymbolTable;

public class PopParameter extends ThreeAddressCode {
    String which;

    public PopParameter(String which, AST source) {
        super(source);
        this.which = which;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", DOUBLE_INDENT, "PopParameter", which);
    }

    @Override
    public <T, E> T accept(ThreeAddressCodeVisitor<T, E> visitor, SymbolTable currentSymbolTable, E extra) {
        return visitor.visit(this, currentSymbolTable, extra);
    }
}
