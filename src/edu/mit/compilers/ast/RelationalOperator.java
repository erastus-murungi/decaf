package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;

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
}