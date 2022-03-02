package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class Len extends Expression {
    final public Name nameId;

    public Len(TokenPosition tokenPosition, Name nameId) {
        super(tokenPosition);
        this.tokenPosition = tokenPosition;
        this.nameId = nameId;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("id", nameId));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "Len{" + "nameId=" + nameId + '}';
    }
}
