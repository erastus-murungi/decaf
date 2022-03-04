package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class ParenthesizedExpression extends Expression {
    public Expression expression;

    public ParenthesizedExpression(TokenPosition tokenPosition, Expression expression) {
        super(tokenPosition);
        this.expression = expression;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("expression", expression));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "ParenthesizedExpression{" + "expression=" + expression + '}';
    }

    @Override
    public void accept(Visitor visitor, SymbolTable<String, Descriptor> curSymbolTable) {
        visitor.visit(this, curSymbolTable);
    }
}
