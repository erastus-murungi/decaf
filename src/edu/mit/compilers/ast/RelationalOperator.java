package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;

public class RelationalOperator extends BinOperator {
    public RelationalOperator(TokenPosition tokenPosition, @DecafScanner.RelationalOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        switch (op) {
            case DecafScanner.LT: return "Lt";
            case DecafScanner.GT: return "Gt";
            case DecafScanner.GEQ: return "GtE";
            case DecafScanner.LEQ : return  "LtE";
            default: throw new IllegalArgumentException("please register relational operator: " + op);
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return null;
    }

    @Override
    public String getSourceCode() {
        switch (op) {
            case DecafScanner.LT: return "<";
            case DecafScanner.GT: return ">";
            case DecafScanner.GEQ: return ">=";
            case DecafScanner.LEQ: return "<=";
            default: throw new IllegalArgumentException("please register relational operator: " + op);
        }
    }
}