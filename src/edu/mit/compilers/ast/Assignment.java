package edu.mit.compilers.ast;

import java.util.List;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

public class Assignment extends AST {
    public Location location;
    public AssignExpr assignExpr;
    public AssignOperator assignOperator;

    public Assignment (Location location, AssignExpr assignmentExpr, AssignOperator assignOperator){
        this.location = location;
        this.assignExpr = assignmentExpr;
        this.assignOperator = assignOperator;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("location", location), new Pair<>("assignExpr", assignExpr), new Pair<>("assignOperator", assignOperator));
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
    public String getSourceCode() {
        return String.format("%s %s", location.getSourceCode(), assignExpr.getSourceCode());
    }

}
