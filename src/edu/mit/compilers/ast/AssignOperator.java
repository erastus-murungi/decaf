package edu.mit.compilers.ast;


import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;

public class AssignOperator extends Operator {
    public AssignOperator(TokenPosition tokenPosition, @DecafScanner.AssignOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        switch (op) {
            case DecafScanner.ASSIGN : return "Assign";
            case DecafScanner.ADD_ASSIGN : return "AugmentedAdd";
            case DecafScanner.MINUS_ASSIGN : return "AugmentedSub";
            case DecafScanner.MULTIPLY_ASSIGN : return "AugmentedMul";
            default : throw new IllegalArgumentException("please register assign operator: " + op);
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return null;
    }
}
