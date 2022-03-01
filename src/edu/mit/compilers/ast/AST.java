package edu.mit.compilers.ast;

import edu.mit.compilers.utils.Pair;

import java.util.List;

public abstract class AST {

    public abstract List<Pair<String, AST>> getChildren();

    public abstract boolean isTerminal();

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
