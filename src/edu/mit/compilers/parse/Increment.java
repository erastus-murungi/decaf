package edu.mit.compilers.parse;


import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;

public class Increment extends AssignExpr {
    private final IncrementType incrementType;

    public enum IncrementType{
        INCREMENT,
        DECREMENT
    }

    public Increment(IncrementType incrementType) {
        this.incrementType = incrementType;
    }

    @Override
    public List<Pair<String, Node>> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    @Override
    public String toString() {
        return (incrementType == IncrementType.INCREMENT) ? "Increment{}" : "Decrement{}";
    }
}
