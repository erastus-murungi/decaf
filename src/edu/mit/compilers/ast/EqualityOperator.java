package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;

public class EqualityOperator extends BinOperator {

    public EqualityOperator(TokenPosition tokenPosition, @DecafScanner.EqualityOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        return switch (op) {
            case DecafScanner.EQ -> "Eq";
            case DecafScanner.NEQ -> "NotEq";
            default -> throw new IllegalArgumentException("please register equality operator: " + op);
    };
}

    @Override
    public void accept(Visitor visitor, SymbolTable<String, Descriptor> curSymbolTable) {
       // do nothing
    }
}