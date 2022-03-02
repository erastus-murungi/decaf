package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class UnaryOpExpression extends Expression {
    final public UnaryOperator op;
    public Expression operand;

    public UnaryOpExpression(UnaryOperator op, Expression operand) {
        super(op.tokenPosition);
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

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable<String, Descriptor> curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }
}
