package decaf.ast;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignableValue;
import decaf.grammar.DecafScanner;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class EqualityOperator extends BinOperator {

    public EqualityOperator(TokenPosition tokenPosition, @DecafScanner.EqualityOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        switch (label) {
            case DecafScanner.EQ:
                return "Eq";
            case DecafScanner.NEQ:
                return "NotEq";
            default:
                throw new IllegalArgumentException("please register equality operator: " + label);
        }
    }

    @Override
    public <T> T accept(AstVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return null;
    }

    @Override
    public String getSourceCode() {
        switch (label) {
            case DecafScanner.EQ:
                return "==";
            case DecafScanner.NEQ:
                return "!=";
            default:
                throw new IllegalArgumentException("please register equality operator: " + label);
        }
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
        return null;
    }
}