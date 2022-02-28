package edu.mit.compilers.parse;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;

public class BooleanLiteral extends Literal {

    public BooleanLiteral(TokenPosition tokenPosition, @DecafScanner.BooleanLiteral String literal) {
        super(tokenPosition, literal);
    }

    @Override
    public String toString() {
        return "BooleanLiteral{" + "literal='" + literal + '\'' + '}';
    }
}
