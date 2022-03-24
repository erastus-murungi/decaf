package edu.mit.compilers.cfg;

import edu.mit.compilers.symbolTable.SymbolTable;

public class NOP extends CFGNonConditional {
    public NOP() {
        super(null);
    }

    public<T> T accept(CFGVisitor<T> visitor, SymbolTable symbolTable) {
        return visitor.visit(this, symbolTable);
    };

    @Override
    public String getLabel() {
        return "NOP";
    }
}
