package edu.mit.compilers.ast;

import java.util.List;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.ir.ASTVisitor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;

public class LocationArray extends Location implements HasExpression {
    public Expression expression;

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
    public <T> T accept(ASTVisitor<T> ASTVisitor, SymbolTable currentSymbolTable) {
        return ASTVisitor.visit(this, currentSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }

    @Override
    public String toString() {
        return "LocationArray{" + "name=" + name + ", expression=" + expression + '}';
    }

    @Override
    public String getSourceCode() {
        return String.format("%s[%s]", name.getSourceCode(), expression.getSourceCode());
    }

    @Override
    public List<Expression> getExpression() {
        return List.of(expression);
    }

    @Override
    public void compareAndSwapExpression(Expression oldExpr, Expression newExpr) {
        if (expression == oldExpr)
            expression = newExpr;
    }
}
