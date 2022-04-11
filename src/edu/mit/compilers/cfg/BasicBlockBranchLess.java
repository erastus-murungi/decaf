package edu.mit.compilers.cfg;

import edu.mit.compilers.symbolTable.SymbolTable;

import java.util.List;

public class BasicBlockBranchLess extends BasicBlock {
    public BasicBlock autoChild;

    public BasicBlockBranchLess(BasicBlock autoChild) {
        this.autoChild = autoChild;
    }

    @Override
    public List<BasicBlock> getSuccessors() {
        return List.of(autoChild);
    }

    public BasicBlockBranchLess() {
    }

    @Override
    public <T> T accept(BasicBlockVisitor<T> visitor, SymbolTable symbolTable) {
        return visitor.visit(this, symbolTable);
    }
}
