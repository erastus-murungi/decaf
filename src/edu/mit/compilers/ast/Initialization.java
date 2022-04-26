package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class Initialization extends Statement implements HasExpression  {
    public final Name initId;
    public Expression initExpression;

    public Initialization(Name initId, Expression initExpression) {
        super(initId.tokenPosition);
        this.initId = initId;
        this.initExpression = initExpression;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("initId", initId), new Pair<>("initExpression", initExpression));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable currentSymbolTable) {
        return visitor.visit(this, currentSymbolTable);
    }

    @Override
    public String getSourceCode() {
        return String.format("%s = %s", initId.getSourceCode(), initExpression.getSourceCode());
    }

    @Override
    public String toString() {
        return "Initialization{" +
                "initId=" + initId +
                ", initExpression=" + initExpression +
                '}';
    }

    @Override
    public List<Expression> getExpression() {
        return List.of(initExpression);
    }

    @Override
    public void compareAndSwapExpression(Expression oldExpr, Expression newExpr) {
        if (initExpression == oldExpr)
            initExpression = newExpr;
    }
}
