package edu.mit.compilers.parse;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.DecafScanner.ConditionalOperator;
import edu.mit.compilers.grammar.TokenPosition;

public class ConditionalOp extends BinOp {

    public ConditionalOp(TokenPosition tokenPosition, @ConditionalOperator String op) {
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