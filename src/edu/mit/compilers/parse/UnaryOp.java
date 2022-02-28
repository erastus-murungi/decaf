package edu.mit.compilers.parse;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.DecafScanner.UnaryOperator;
import edu.mit.compilers.grammar.TokenPosition;

public class UnaryOp extends Op {
    public UnaryOp(TokenPosition tokenPosition, @UnaryOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        return switch (op) {
            case DecafScanner.MINUS -> "Neg";
            case DecafScanner.NOT -> "Not";
            default -> throw new IllegalArgumentException("please register unary operator: " + op);
        };
    }
}