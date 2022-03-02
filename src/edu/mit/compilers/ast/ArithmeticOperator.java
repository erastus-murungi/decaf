package edu.mit.compilers.ast;

import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;

public class ArithmeticOperator extends BinOperator {
    public ArithmeticOperator(TokenPosition tokenPosition, @DecafScanner.ArithmeticOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        return switch (op) {
            case DecafScanner.PLUS -> "Add";
            case DecafScanner.MINUS -> "Sub";
            case DecafScanner.MULTIPLY -> "Multiply";
            case DecafScanner.DIVIDE -> "Divide";
            case DecafScanner.MOD -> "Mod";
            default -> throw new IllegalArgumentException("please register a display string for: " + op);
        };
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable<String, Descriptor> curSymbolTable) {
        return null;
    }
}
