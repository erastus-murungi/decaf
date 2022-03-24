package edu.mit.compilers.cfg;

import edu.mit.compilers.symbolTable.SymbolTable;

public interface CFGVisitor<T> {
    T visit(CFGNonConditional cfgNonConditional, SymbolTable symbolTable);
    T visit(CFGConditional cfgConditional, SymbolTable symbolTable);
    T visit(NOP nop, SymbolTable symbolTable);
}
