package edu.mit.compilers.ast;


import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;

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
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return null;
    }

    @Override
    public String getSourceCode() {
        return switch (op) {
            case DecafScanner.PLUS -> "+";
            case DecafScanner.MINUS -> "-";
            case DecafScanner.MULTIPLY -> "*";
            case DecafScanner.DIVIDE -> "/";
            case DecafScanner.MOD -> "%";
            default -> throw new IllegalArgumentException("please register a display string for: " + op);
        };
    }
}
