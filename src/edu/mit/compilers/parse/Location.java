package edu.mit.compilers.parse;

import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;

public class Location extends Expr {
    public final Name name;

    public Location(Name name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name.toString();
    }

    @Override
    public List<Pair<String, Node>> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public boolean isTerminal() {
        return true;
    }
}
