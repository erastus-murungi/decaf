package edu.mit.compilers.cfg;

import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.List;

public class CFGNonConditional extends CFGBlock {
    public CFGBlock autoChild;

    public CFGNonConditional(CFGBlock autoChild) {
        this.autoChild = autoChild;
    }

    @Override
    public List<CFGBlock> getSuccessors() {
        return List.of(autoChild);
    }

    public CFGNonConditional() {
    }

    @Override
    public <T> T accept(CFGVisitor<T> visitor, SymbolTable symbolTable) {
        return visitor.visit(this, symbolTable);
    }
}
