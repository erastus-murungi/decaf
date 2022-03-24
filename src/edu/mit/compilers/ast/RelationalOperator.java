package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;

public class RelationalOperator extends BinOperator {
    public RelationalOperator(TokenPosition tokenPosition, @DecafScanner.RelationalOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        return switch (op) {
            case DecafScanner.LT -> "Lt";
            case DecafScanner.GT -> "Gt";
            case DecafScanner.GEQ -> "GtE";
            case DecafScanner.LEQ -> "LtE";
            default -> throw new IllegalArgumentException("please register relational operator: " + op);
        };
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return null;
    }

    @Override
    public String getSourceCode() {
        return switch (op) {
            case DecafScanner.LT -> "<";
            case DecafScanner.GT -> ">";
            case DecafScanner.GEQ -> ">=";
            case DecafScanner.LEQ -> "<=";
            default -> throw new IllegalArgumentException("please register relational operator: " + op);
        };
    }
}