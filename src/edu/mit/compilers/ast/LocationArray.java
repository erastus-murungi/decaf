package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class LocationArray extends Location {
    public final Expression expression;
    public LocationArray(Name name, Expression expression) {
        super(name);
        this.expression = expression;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("id", name), new Pair<>("expression", expression));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable currentSymbolTable) {
        return visitor.visit(this, currentSymbolTable);
    }

    @Override
    public String toString() {
        return "LocationArray{" + "name=" + name + ", expression=" + expression + '}';
    }
}
