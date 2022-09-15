package decaf.ast;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignableValue;
import decaf.grammar.DecafScanner;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class UnaryOperator extends Operator {
    public UnaryOperator(TokenPosition tokenPosition, @DecafScanner.UnaryOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        switch (label) {
            case DecafScanner.MINUS:
                return "Neg";
            case DecafScanner.NOT:
                return "Not";
            default:
                throw new IllegalArgumentException("please register unary operator: " + label);
        }
    }

    @Override
    public <T> T accept(AstVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return null;
    }

    @Override
    public String getSourceCode() {
        switch (label) {
            case DecafScanner.MINUS:
                return "-";
            case DecafScanner.NOT:
                return "!";
            default:
                throw new IllegalArgumentException("please register unary operator: " + label);
        }
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
        return null;
    }
}