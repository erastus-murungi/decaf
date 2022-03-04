package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 

public class DecimalLiteral extends IntLiteral {
    public DecimalLiteral(TokenPosition tokenPosition, String literalToken) {
        super(tokenPosition, literalToken);
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }

    @Override
    public Long convertToLong() {
        return Long.parseLong(literal);
    }
}