package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;

public class ConditionalOperator extends BinOperator {

    public ConditionalOperator(TokenPosition tokenPosition, @DecafScanner.ConditionalOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        return switch (op) {
            case DecafScanner.CONDITIONAL_OR -> "Or";
            case DecafScanner.CONDITIONAL_AND -> "And";
            default -> throw new IllegalArgumentException("please register conditional operator: " + op);
        };
    }
}