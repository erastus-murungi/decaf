package edu.mit.compilers.ast;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.ASTVisitor;
import edu.mit.compilers.symboltable.SymbolTable;

public class RelationalOperator extends BinOperator {
    public RelationalOperator(TokenPosition tokenPosition, @DecafScanner.RelationalOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        switch (label) {
            case DecafScanner.LT:
                return "Lt";
            case DecafScanner.GT:
                return "Gt";
            case DecafScanner.GEQ:
                return "GtE";
            case DecafScanner.LEQ:
                return "LtE";
            default:
                throw new IllegalArgumentException("please register relational operator: " + label);
        }
    }

    @Override
    public <T> T accept(ASTVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return null;
    }

    @Override
    public String getSourceCode() {
        switch (label) {
            case DecafScanner.LT:
                return "<";
            case DecafScanner.GT:
                return ">";
            case DecafScanner.GEQ:
                return ">=";
            case DecafScanner.LEQ:
                return "<=";
            default:
                throw new IllegalArgumentException("please register relational operator: " + label);
        }
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return null;
    }
}