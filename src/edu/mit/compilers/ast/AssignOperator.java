package edu.mit.compilers.ast;


import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;

public class AssignOperator extends Operator {
    public AssignOperator(TokenPosition tokenPosition, @DecafScanner.AssignOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        switch (label) {
            case DecafScanner.ASSIGN: return "Assign";
            case DecafScanner.ADD_ASSIGN: return "AugmentedAdd";
            case DecafScanner.MINUS_ASSIGN: return "AugmentedSub";
            case DecafScanner.MULTIPLY_ASSIGN: return "AugmentedMul";
            default: throw new IllegalArgumentException("please register assign operator: " + label);
        }
    }

    @Override
    public Type getType() {
        return Type.Undefined;
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return null;
    }

    @Override
    public String getSourceCode() {
        switch (label) {
            case DecafScanner.ASSIGN: return  "=";
            case DecafScanner.ADD_ASSIGN: return  "+=";
            case DecafScanner.MINUS_ASSIGN: return  "-=";
            case DecafScanner.MULTIPLY_ASSIGN: return  "*=";
            default: throw new IllegalArgumentException("please register assign operator: " + label);
        }
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return null;
    }
}
