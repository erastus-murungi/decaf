package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;

public class ConditionalOperator extends BinOperator {

    public ConditionalOperator(TokenPosition tokenPosition, @DecafScanner.ConditionalOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        return switch (op) {
            case DecafScanner.CONDITIONAL_OR -> "Or";
            case DecafScanner.CONDITIONAL_AND -> "And";
            default -> throw new IllegalArgumentException("please register conditional operator: " + op);
        };
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable<String, Descriptor> curSymbolTable) {
        return null;
    }
}