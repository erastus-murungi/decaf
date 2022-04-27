package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class AssignOpExpr extends AssignExpr implements HasExpression {
    final public AssignOperator assignOp;
    public Expression expression;

    public AssignOpExpr(TokenPosition tokenPosition, AssignOperator assignOp, Expression expression) {
        super(tokenPosition, expression);
        this.assignOp = assignOp;
        this.expression = expression;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("assignOp", assignOp), new Pair<>("expression", expression));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "AssignOpExpr{" + "assignOp=" + assignOp + ", expression=" + expression + '}';
    }

    @Override
    public String getSourceCode() {
        return String.format("%s %s", assignOp.getSourceCode(), expression.getSourceCode());
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }

    @Override
    public String getOperator() {
        return assignOp.op;
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
