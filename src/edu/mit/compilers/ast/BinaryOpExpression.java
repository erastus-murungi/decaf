package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

import java.util.HashMap;
import java.util.List;

public class BinaryOpExpression extends Expression {
    static HashMap<String, Integer> operatorPrecedence = new HashMap<>();
    static {
        operatorPrecedence.put("*", 13);
        operatorPrecedence.put("/", 13);
        operatorPrecedence.put("%", 13);
        operatorPrecedence.put("+", 12);
        operatorPrecedence.put("-", 12);
        operatorPrecedence.put("<", 10);
        operatorPrecedence.put("<=", 10);
        operatorPrecedence.put(">", 10);
        operatorPrecedence.put(">=", 10);
        operatorPrecedence.put("==", 9);
        operatorPrecedence.put("!=", 9);
        operatorPrecedence.put("&&", 5);
        operatorPrecedence.put("||", 4);
    }
    public Expression lhs;
    public BinOperator op;
    public Expression rhs;

    private BinaryOpExpression(Expression lhs, BinOperator binaryOp, Expression rhs) {
        super(lhs.tokenPosition);
        this.lhs = lhs;
        this.rhs = rhs;
        this.op = binaryOp;
    }

    public static BinaryOpExpression of(Expression lhs, BinOperator binaryOp, Expression rhs) {
        BinaryOpExpression binaryOpExpression = new BinaryOpExpression(lhs, binaryOp, rhs);
        binaryOpExpression.lhs = lhs;
        binaryOpExpression.rhs = rhs;
        binaryOpExpression.op = binaryOp;
        return maybeRotate(binaryOpExpression);
    }

    private static BinaryOpExpression maybeRotate(BinaryOpExpression parent) {
        if ((parent.rhs instanceof BinaryOpExpression)) {
            BinaryOpExpression child = (BinaryOpExpression) parent.rhs;
            if (operatorPrecedence.get(parent.op.op).equals(operatorPrecedence.get(child.op.op))) {
                return new BinaryOpExpression(new BinaryOpExpression(parent.lhs, parent.op, child.lhs), child.op, child.rhs);
            }
        }
        return parent;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("op", op), new Pair<>("lhs", lhs), new Pair<>("rhs", rhs));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "BinaryOpExpression{" + "lhs=" + lhs + ", op=" + op + ", rhs=" + rhs + '}';
    }

    @Override
    public String getSourceCode() {
        return String.format("%s %s %s", lhs.getSourceCode(), op.getSourceCode(), rhs.getSourceCode());
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }
}
