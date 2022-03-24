package edu.mit.compilers.ast;

import java.util.Collections;
import java.util.List;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

public class LocationVariable extends Location {
    public LocationVariable(Name name) {
        super(name);
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }


    @Override
    public boolean isTerminal() {
        return true;
    }

    @Override
    public String toString() {
        return "LocationVariable{" + "name=" + name + '}';
    }

    @Override
    public String getSourceCode() {
        return name.getSourceCode();
    }
}
