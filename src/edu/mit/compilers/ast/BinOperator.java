package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.DecafScanner.BinaryOperator;
import edu.mit.compilers.grammar.TokenPosition;

public abstract class BinOperator extends Operator {

    public BinOperator(TokenPosition tokenPosition, @BinaryOperator String op) {
        super(tokenPosition, op);
    }
}
