package edu.mit.compilers.ast;

import edu.mit.compilers.utils.Pair;

import java.util.List;

public class UnaryOpExpression extends Expression {
    final UnaryOperator op;
    final Expression operand;

    public UnaryOpExpression(UnaryOperator op, Expression operand) {
        this.op = op;
        this.operand = operand;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("op", op), new Pair<>("operand", operand));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "UnaryOpExpression{" + "op=" + op + ", operand=" + operand + '}';
    }
}