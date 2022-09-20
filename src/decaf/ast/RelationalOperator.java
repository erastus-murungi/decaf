package decaf.ast;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.grammar.DecafScanner;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

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
    public <T> T accept(AstVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
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

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignable resultLocation) {
        return null;
    }
}