package edu.mit.compilers.cfg;

import edu.mit.compilers.symbolTable.SymbolTable;

public class NOP extends CFGBlock {
 
    public NOP(){

    }

    public<T> T accept(CFGVisitor<T> visitor, SymbolTable symbolTable) {
        return visitor.visit(this, symbolTable);
    };
}
