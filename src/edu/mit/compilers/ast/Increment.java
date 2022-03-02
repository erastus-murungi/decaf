package edu.mit.compilers.ast;


import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;

public class Increment extends AssignExpr {
    public Increment(TokenPosition tokenPosition) {
        super(tokenPosition, null);
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
    public String toString() {
        return "Increment{}";
    }
}
