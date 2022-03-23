package edu.mit.compilers.cfg;

import edu.mit.compilers.symbolTable.SymbolTable;

public interface CFGVisitor<T> {
    T visit(CFGBlock cfgBlock, SymbolTable symbolTable);
    T visit(NOP nop, SymbolTable symbolTable);
}
