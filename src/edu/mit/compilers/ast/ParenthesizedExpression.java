package edu.mit.compilers.ast;

import edu.mit.compilers.utils.Pair;

import java.util.List;

public class ParenthesizedExpression extends Expression {
    final Expression expression;

    public ParenthesizedExpression(Expression expression) {
        this.expression = expression;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("expression", expression));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "ParenthesizedExpression{" + "expression=" + expression + '}';
    }
}
