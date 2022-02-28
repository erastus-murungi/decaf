package edu.mit.compilers.parse;

import edu.mit.compilers.utils.Pair;

import java.util.List;

public class LocationArray extends Location {
    final Expr expr;
    public LocationArray(Name name, Expr expr) {
        super(name);
        this.expr = expr;
    }

    @Override
    public List<Pair<String, Node>> getChildren() {
        return List.of(new Pair<>("id", name), new Pair<>("expr", expr));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "LocationArray{" + "name=" + name + ", expr=" + expr + '}';
    }
}
