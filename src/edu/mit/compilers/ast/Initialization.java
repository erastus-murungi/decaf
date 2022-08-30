package edu.mit.compilers.ast;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class Initialization extends Statement implements HasExpression  {
    public final Name initLocation;
    public Expression initExpression;

    public Initialization(Name initLocation, Expression initExpression) {
        super(initLocation.tokenPosition);
        this.initLocation = initLocation;
        this.initExpression = initExpression;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("initLocation", initLocation), new Pair<>("initExpression", initExpression));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable currentSymbolTable) {
        return visitor.visit(this, currentSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }

    @Override
    public String getSourceCode() {
        return String.format("%s = %s", initLocation.getSourceCode(), initExpression.getSourceCode());
    }

    @Override
    public String toString() {
        return "Initialization{" +
                "initLocation=" + initLocation +
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
