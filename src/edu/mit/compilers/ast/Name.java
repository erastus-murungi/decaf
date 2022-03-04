package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;


/* A variable name. id holds the name as a string, and ctx is one of the following types. (Load, Store) */
public class Name extends AST {
    public final String id;
    public final TokenPosition tokenPosition;
    final ExprContext context;

    public Name(String id, TokenPosition tokenPosition, ExprContext context) {
        this.id = id;
        this.tokenPosition = tokenPosition;
        this.context = context;
    }

    @Override
    public String toString() {
        return "Name{" + "id='" + id + '\'' + ", context=" + context + '}';
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    @Override
    public void accept(Visitor visitor, SymbolTable<String, Descriptor> curSymbolTable) {
        visitor.visit(this, curSymbolTable);
    }
}
