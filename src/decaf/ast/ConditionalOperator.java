package decaf.ast;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.codegen.names.IrAssignable;
import decaf.grammar.DecafScanner;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class ConditionalOperator extends BinOperator {
    public ConditionalOperator(TokenPosition tokenPosition, @DecafScanner.ConditionalOperator String op) {
        super(tokenPosition, op);
    }

    @Override
    public String opRep() {
        return switch (label) {
            case DecafScanner.CONDITIONAL_OR -> "Or";
            case DecafScanner.CONDITIONAL_AND -> "And";
            default -> throw new IllegalArgumentException("please register conditional operator: " + label);
        };
    }

    @Override
    public <T> T accept(AstVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return null;
    }

    @Override
    public String getSourceCode() {
        return switch (label) {
            case DecafScanner.CONDITIONAL_OR -> "||";
            case DecafScanner.CONDITIONAL_AND -> "&&";
            default -> throw new IllegalArgumentException("please register conditional operator: " + label);
        };
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignable resultLocation) {
        return null;
    }
}