package edu.mit.compilers.ast;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;

public class ExpressionParameter extends MethodCallParameter implements HasExpression {
    public Expression expression;

    @Override
    public Type getType() {
        return expression.getType();
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return Collections.singletonList(new Pair<>("expression", expression));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    public ExpressionParameter(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "ExpressionParameter{" + "expression=" + expression + '}';
    }

    @Override
    public String getSourceCode() {
        return expression.getSourceCode();
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
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
