package edu.mit.compilers.parse;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;

public class BuiltinType extends Literal {
    public BuiltinType(TokenPosition tokenPosition, @DecafScanner.BuiltinTypes String literal) {
        super(tokenPosition, literal);
    }
}
