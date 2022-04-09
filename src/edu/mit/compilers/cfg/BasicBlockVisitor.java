package edu.mit.compilers.cfg;

import edu.mit.compilers.symbolTable.SymbolTable;

public interface BasicBlockVisitor<T> {
    T visit(BasicBlockBranchLess basicBlockBranchLess, SymbolTable symbolTable);
    T visit(BasicBlockWithBranch basicBlockWithBranch, SymbolTable symbolTable);
    T visit(NOP nop, SymbolTable symbolTable);
}
