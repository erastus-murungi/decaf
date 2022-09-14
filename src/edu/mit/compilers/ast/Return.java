package edu.mit.compilers.ast;

import static edu.mit.compilers.grammar.DecafScanner.RESERVED_RETURN;

import java.util.Collections;
import java.util.List;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.ASTVisitor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;

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
        return String.format("%s %s", RESERVED_RETURN, retExpression == null ? "" : retExpression.getSourceCode());
    }

    @Override
    public <T> T accept(ASTVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
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
