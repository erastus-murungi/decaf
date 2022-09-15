package decaf.ast;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignableValue;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class DecimalLiteral extends IntLiteral {
    public DecimalLiteral(TokenPosition tokenPosition, String literalToken) {
        super(tokenPosition, literalToken);
    }

    @Override
    public <T> T accept(AstVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }

    @Override
    public Long convertToLong() {
        return Long.parseLong(literal);
    }
}