package decaf.ast;

import java.util.List;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignableValue;
import decaf.common.Pair;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class Initialization extends Statement implements HasExpression {
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
    public <T> T accept(AstVisitor<T> ASTVisitor, SymbolTable currentSymbolTable) {
        return ASTVisitor.visit(this, currentSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
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
