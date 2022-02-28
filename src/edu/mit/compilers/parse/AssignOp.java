package edu.mit.compilers.parse;


import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.DecafScanner.AssignOperator;
import edu.mit.compilers.grammar.TokenPosition;

public class AssignOp extends Op {
    public AssignOp(TokenPosition tokenPosition, @AssignOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        return switch (op) {
            case DecafScanner.ASSIGN -> "Assign";
            case DecafScanner.ADD_ASSIGN -> "AugmentedAdd";
            case DecafScanner.MINUS_ASSIGN -> "AugmentedSub";
            case DecafScanner.MULTIPLY_ASSIGN -> "AugmentedMul";
            default -> throw new IllegalArgumentException("please register assign operator: " + op);
        };
    }
}
