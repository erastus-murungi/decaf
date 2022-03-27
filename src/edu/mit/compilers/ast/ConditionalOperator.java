package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;

public class ConditionalOperator extends BinOperator {
    public ConditionalOperator(TokenPosition tokenPosition, @DecafScanner.ConditionalOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        switch (op) {
            case DecafScanner.CONDITIONAL_OR: return "Or";
            case DecafScanner.CONDITIONAL_AND: return "And";
            default: throw new IllegalArgumentException("please register conditional operator: " + op);
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return null;
    }

    @Override
    public String getSourceCode() {
        switch (op) {
            case DecafScanner.CONDITIONAL_OR: return "||";
            case DecafScanner.CONDITIONAL_AND: return "&&";
            default: throw new IllegalArgumentException("please register conditional operator: " + op);
        }
    }
}