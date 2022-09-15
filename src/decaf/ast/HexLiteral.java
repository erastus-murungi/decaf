package decaf.ast;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignableValue;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;


public class HexLiteral extends IntLiteral {
    public HexLiteral(TokenPosition tokenPosition, String hexLiteral) {
        super(tokenPosition, hexLiteral);
    }

    @Override
    public Long convertToLong() {
        return Long.parseLong(literal.substring(2), 16);
    }

    public <T> T accept(AstVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }
}