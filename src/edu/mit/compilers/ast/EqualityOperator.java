package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;

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
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return null;
    }

    @Override
    public String getSourceCode() {
        return switch (op) {
            case DecafScanner.EQ -> "==";
            case DecafScanner.NEQ -> "!=";
            default -> throw new IllegalArgumentException("please register equality operator: " + op);
        };
    }
}