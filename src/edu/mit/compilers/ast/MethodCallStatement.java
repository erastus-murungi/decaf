package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class MethodCallStatement extends Statement {
    public final MethodCall methodCall;

    public MethodCallStatement(TokenPosition tokenPosition, MethodCall methodCall) {
        super(tokenPosition);
        this.methodCall = methodCall;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return methodCall.getChildren();
    }

    @Override
    public String toString() {
        return methodCall.toString();
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public void accept(Visitor visitor, SymbolTable<String, Descriptor> curSymbolTable) {
        visitor.visit(this, curSymbolTable);
    }
}
