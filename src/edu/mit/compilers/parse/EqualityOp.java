package edu.mit.compilers.parse;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.DecafScanner.EqualityOperator;
import edu.mit.compilers.grammar.TokenPosition;

public class EqualityOp extends BinOp {

    public EqualityOp(TokenPosition tokenPosition, @EqualityOperator String op) {
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
}