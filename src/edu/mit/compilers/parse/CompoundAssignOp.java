
package edu.mit.compilers.parse;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.DecafScanner.CompoundAssignOperator;
import edu.mit.compilers.grammar.TokenPosition;

public class CompoundAssignOp extends Op {
    public CompoundAssignOp(TokenPosition tokenPosition, @CompoundAssignOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        return switch (op) {
            case DecafScanner.ADD_ASSIGN -> "AugmentedAdd";
            case DecafScanner.MINUS_ASSIGN -> "AugmentedSub";
            case DecafScanner.MULTIPLY_ASSIGN -> "AugmentedMul";
            default -> throw new IllegalArgumentException("please register compound assign operator: " + op);
        };
    }
}