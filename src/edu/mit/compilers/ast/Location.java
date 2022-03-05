package edu.mit.compilers.ast;

public abstract class Location extends Expression {
    public final Name name;

    public Location(Name name) {
        super(name.tokenPosition);
        this.name = name;
    }
}
