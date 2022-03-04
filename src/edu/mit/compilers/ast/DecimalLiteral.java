package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;

public class DecimalLiteral extends IntLiteral {
    public DecimalLiteral(TokenPosition tokenPosition, String literalToken) {
        super(tokenPosition, literalToken);
    }

    @Override
    public void accept(Visitor visitor, SymbolTable<String, Descriptor> curSymbolTable) {
        visitor.visit(this, curSymbolTable);
    }

    @Override
    public Long convertToLong() {
        return Long.parseLong(literal);
    }
}