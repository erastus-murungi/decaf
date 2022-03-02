package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;

public class Location extends Expression {
    public final Name name;

    public Location(Name name) {
        super(name.tokenPosition);
        this.name = name;
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
        return "Location{" + "id=" + name.id + '}';
    }
}
