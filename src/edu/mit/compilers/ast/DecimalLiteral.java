package edu.mit.compilers.ast;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.ASTVisitor;
import edu.mit.compilers.symboltable.SymbolTable;

public class DecimalLiteral extends IntLiteral {
    public DecimalLiteral(TokenPosition tokenPosition, String literalToken) {
        super(tokenPosition, literalToken);
    }

    @Override
    public <T> T accept(ASTVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
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