package edu.mit.compilers.parse;

import edu.mit.compilers.utils.Pair;

import java.util.List;

public abstract class Node {

    public abstract List<Pair<String, Node>> getChildren();

    public abstract boolean isTerminal();

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
