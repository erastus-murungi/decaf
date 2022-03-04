package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.utils.Pair;

import java.util.List;

public abstract class AST {

    public abstract List<Pair<String, AST>> getChildren();

    public abstract boolean isTerminal();

    public abstract <T> T accept(Visitor<T> visitor, SymbolTable currentSymbolTable);

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
