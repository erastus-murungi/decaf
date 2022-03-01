
package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;

public class CompoundAssignOperator extends Operator {
    public CompoundAssignOperator(TokenPosition tokenPosition, @DecafScanner.CompoundAssignOperator String op) {
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