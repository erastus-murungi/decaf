package edu.mit.compilers.parse;

import edu.mit.compilers.grammar.DecafScanner.BinaryOperator;
import edu.mit.compilers.grammar.TokenPosition;

public abstract class BinOp extends Op {

    public BinOp(TokenPosition tokenPosition, @BinaryOperator String op) {
        super(tokenPosition, op);
    }
}
