package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;

public abstract class AssignExpr extends AST {
    public TokenPosition tokenPosition;
    public Expression expression;

    public abstract String getOperator();

    public AssignExpr(TokenPosition tokenPosition, Expression expression) {
        this.tokenPosition = tokenPosition;
        this.expression = expression;
    }


}
