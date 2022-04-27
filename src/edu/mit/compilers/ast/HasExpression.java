package edu.mit.compilers.ast;

import java.util.List;

public interface HasExpression {
    public List<Expression> getExpression();

    public void compareAndSwapExpression(Expression oldExpr, Expression newExpr);
}
