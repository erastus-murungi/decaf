package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class BinaryOpExpression extends Expression {
    public Expression lhs;
    public BinOperator op;
    public Expression rhs;

    public BinaryOpExpression(Expression lhs, BinOperator binaryOp, Expression rhs) {
        super(lhs.tokenPosition);
        this.lhs = lhs;
        this.rhs = rhs;
        this.op = binaryOp;
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
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
      return visitor.visit(this, curSymbolTable);
    }
}
