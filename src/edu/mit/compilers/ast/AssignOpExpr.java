package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class AssignOpExpr extends AssignExpr {
    final AssignOperator assignOp;
    final Expression expression;

    public AssignOpExpr(AssignOperator assignOp, Expression expression) {
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
}
