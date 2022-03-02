package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class LocationAssignExpr extends Statement {
    final Location location;
    final AssignExpr assignExpr;

    public LocationAssignExpr(TokenPosition tokenPosition, Location location, AssignExpr assignExpr) {
        super(tokenPosition);
        this.location = location;
        this.assignExpr = assignExpr;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("location", location), new Pair<>("assignExpr", assignExpr));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "LocationAssignExpr{" + "location=" + location + ", assignExpr=" + assignExpr + '}';
    }
}