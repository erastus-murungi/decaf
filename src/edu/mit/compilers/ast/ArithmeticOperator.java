package edu.mit.compilers.ast;

import edu.mit.compilers.descriptors.Descriptor;
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
        switch (op) {
            case DecafScanner.PLUS : return "Add";
            case DecafScanner.MINUS : return "Sub";
            case DecafScanner.MULTIPLY : return "Multiply";
            case DecafScanner.DIVIDE : return "Divide";
            case DecafScanner.MOD : return "Mod";
            default : throw new IllegalArgumentException("please register a display string for: " + op);
        }
    }

    @Override
    public void accept(Visitor visitor, SymbolTable<String, Descriptor> curSymbolTable) {
       // do nothing
    }
}
