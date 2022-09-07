package edu.mit.compilers.ast;

import java.util.List;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.ir.ASTVisitor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;

public class Assignment extends AST {
    private final String operator;
    public Location location;
    public AssignExpr assignExpr;

    public Assignment(Location location, AssignExpr assignmentExpr, String operator) {
        this.location = location;
        this.assignExpr = assignmentExpr;
        this.operator = operator;
    }

    public String getOperator() {
        return operator;
    }

    @Override
    public Type getType() {
        return Type.Undefined;
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
    public <T> T accept(ASTVisitor<T> ASTVisitor, SymbolTable currentSymbolTable) {
        return ASTVisitor.visit(this, currentSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }

    @Override
    public String getSourceCode() {
        return String.format("%s %s", location.getSourceCode(), assignExpr.getSourceCode());
    }

    @Override
    public String toString() {
        return "Assignment{" +
                "location=" + location +
                ", assignExpr=" + assignExpr +
                '}';
    }
}
