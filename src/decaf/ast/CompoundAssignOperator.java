package decaf.ast;

import decaf.codegen.names.IrAssignableValue;
import decaf.grammar.DecafScanner;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;
import decaf.codegen.CodegenAstVisitor;

public class CompoundAssignOperator extends Operator {
    public CompoundAssignOperator(TokenPosition tokenPosition, @DecafScanner.CompoundAssignOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        switch (label) {
            case DecafScanner.ADD_ASSIGN:
                return "AugmentedAdd";
            case DecafScanner.MINUS_ASSIGN:
                return "AugmentedSub";
            case DecafScanner.MULTIPLY_ASSIGN:
                return "AugmentedMul";
            default:
                throw new IllegalArgumentException("please register compound assign operator: " + label);
        }
    }

    @Override
    public <T> T accept(AstVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return null;
    }


    @Override
    public String getSourceCode() {
        switch (label) {
            case DecafScanner.ADD_ASSIGN:
                return "+=";
            case DecafScanner.MINUS_ASSIGN:
                return "-=";
            case DecafScanner.MULTIPLY_ASSIGN:
                return "*=";
            default:
                throw new IllegalArgumentException("please register compound assign operator: " + label);
        }
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
        return null;
    }
}