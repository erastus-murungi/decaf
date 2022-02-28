package edu.mit.compilers.parse;


import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;


public abstract class Op extends Node {
    final TokenPosition tokenPosition;
    final String op;

    public Op(TokenPosition tokenPosition, String op) {
        this.tokenPosition = tokenPosition;
        this.op = op;
    }

    @Override
    public List<Pair<String, Node>> getChildren() {
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