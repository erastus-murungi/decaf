package edu.mit.compilers.ast;

import java.util.List;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

public class Assignment extends AST{
    public Location location;
    public AssignExpr assignExpr;

    public Assignment (Location location, AssignExpr assignmentExpr){
        this.location = location;
        this.assignExpr = assignmentExpr;

    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return null;
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable currentSymbolTable) {
        return null;
    }
    
}
