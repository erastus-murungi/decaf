package edu.mit.compilers.ast;


import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;


public abstract class Operator extends AST {
    final TokenPosition tokenPosition;
    public final String op;

    public Operator(TokenPosition tokenPosition, String op) {
        this.tokenPosition = tokenPosition;
        this.op = op;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return Collections.emptyList();
    }

    public abstract String opRep();

    @Override
    public boolean isTerminal() {
        return true;
    }

    @Override
    public String toString() {
        return opRep() + "{}";
    }
}