package edu.mit.compilers.ast;


import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;

public class AssignOperator extends Operator {
    public AssignOperator(TokenPosition tokenPosition, @DecafScanner.AssignOperator String op) {
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
