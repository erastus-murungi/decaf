package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;

public class BuiltinType extends Literal {
    public BuiltinType(TokenPosition tokenPosition, @DecafScanner.BuiltinTypes String literal) {
        super(tokenPosition, literal);
    }
}
