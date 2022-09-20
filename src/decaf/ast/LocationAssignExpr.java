package decaf.ast;

import java.util.List;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.common.Pair;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

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
    public <T> T accept(AstVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignable resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }
}
