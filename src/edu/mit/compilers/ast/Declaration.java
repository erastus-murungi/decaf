package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public abstract class Declaration extends AST {
    @Override
    public List<Pair<String, AST>> getChildren() {
        return null;
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable currentSymbolTable) {
        return null;
    }

    @Override
    public String getSourceCode() {
        return null;
    }
}
