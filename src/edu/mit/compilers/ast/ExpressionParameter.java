package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;

public class ExpressionParameter extends MethodCallParameter {
    final Expression expression;
    @Override
    public List<Pair<String, AST>> getChildren() {
        return Collections.singletonList(new Pair<>("expression", expression));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    public ExpressionParameter(Expression expression) {
        this.expression = expression;
    }

    @Override
    public String toString() {
        return "ExprParameter{" + "expression=" + expression + '}';
    }

    @Override
    public void accept(Visitor visitor, SymbolTable<String, Descriptor> curSymbolTable) {
        visitor.visit(this, curSymbolTable);
    }
}
