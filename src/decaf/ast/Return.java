package decaf.ast;

import java.util.Collections;
import java.util.List;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.common.Pair;
import decaf.grammar.DecafScanner;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class Return extends Statement implements HasExpression {
    public Expression retExpression;

    public Return(TokenPosition tokenPosition, Expression expression) {
        super(tokenPosition);
        this.retExpression = expression;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return (retExpression == null) ? Collections.emptyList() : List.of(new Pair<>("return", retExpression));
    }

    @Override
    public boolean isTerminal() {
        return retExpression == null;
    }

    @Override
    public String toString() {
        return (retExpression == null) ? "Return{}" : "Return{" + "retExpression=" + retExpression + '}';
    }

    @Override
    public String getSourceCode() {
        return String.format("%s %s", DecafScanner.RESERVED_RETURN, retExpression == null ? "" : retExpression.getSourceCode());
    }

    @Override
    public <T> T accept(AstVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignable resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }

    @Override
    public List<Expression> getExpression() {
        if (retExpression == null)
            return Collections.emptyList();
        return List.of(retExpression);
    }

    @Override
    public void compareAndSwapExpression(Expression oldExpr, Expression newExpr) {
        if (retExpression == oldExpr)
            retExpression = newExpr;
    }
}
