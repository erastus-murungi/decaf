package edu.mit.compilers.ast;

import edu.mit.compilers.utils.Pair;

import java.util.List;

public class CompoundAssignOpExpr extends AssignExpr {
    final CompoundAssignOperator compoundAssignOp;
    final Expression expression;

    public CompoundAssignOpExpr(CompoundAssignOperator compoundAssignOp, Expression expression) {
        this.compoundAssignOp = compoundAssignOp;
        this.expression = expression;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("compoundAssign", compoundAssignOp), new Pair<>("expression", expression));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }


    @Override
    public String toString() {
        return "CompoundAssignOpExpr{" + "compoundAssignOp=" + compoundAssignOp + ", expression=" + expression + '}';
    }
}
