package edu.mit.compilers.ast;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class LocationAssignExpr extends Statement {
    public final Location location;
    public final AssignExpr assignExpr;

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

    @Override
    public String getSourceCode() {
        return String.format("%s %s", location.getSourceCode(), assignExpr.getSourceCode());
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }
}
