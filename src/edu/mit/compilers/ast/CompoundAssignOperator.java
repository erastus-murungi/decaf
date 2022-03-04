
package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;

public class CompoundAssignOperator extends Operator {
    public CompoundAssignOperator(TokenPosition tokenPosition, @DecafScanner.CompoundAssignOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        switch (op) {
            case DecafScanner.ADD_ASSIGN : return "AugmentedAdd";
            case DecafScanner.MINUS_ASSIGN : return "AugmentedSub";
            case DecafScanner.MULTIPLY_ASSIGN : return "AugmentedMul";
            default : throw new IllegalArgumentException("please register compound assign operator: " + op);
        }
    }

    @Override
    public void accept(Visitor visitor, SymbolTable<String, Descriptor> curSymbolTable) {
        // do nothing
    }
}