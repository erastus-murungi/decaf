package edu.mit.compilers.ast;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class UnaryOpExpression extends Expression implements HasExpression  {
    private UnaryOperator unaryOperator;
    public Expression operand;

    public UnaryOperator getUnaryOperator() {
        return unaryOperator;
    }

    public UnaryOpExpression(UnaryOperator unaryOperator, Expression operand) {
        super(unaryOperator.tokenPosition);
        this.unaryOperator = unaryOperator;
        this.operand = operand;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("op", unaryOperator), new Pair<>("operand", operand));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "UnaryOpExpression{" + "op=" + unaryOperator + ", operand=" + operand + '}';
    }

    @Override
    public String getSourceCode() {
        return String.format("%s(%s)", unaryOperator.getSourceCode(), operand.getSourceCode());
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }

    @Override
    public List<Expression> getExpression() {
        return List.of(operand);
    }

    @Override
    public void compareAndSwapExpression(Expression oldExpr, Expression newExpr) {
        if (operand == oldExpr)
            operand = newExpr;
    }
}
