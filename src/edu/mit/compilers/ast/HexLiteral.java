package edu.mit.compilers.ast;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;


public class HexLiteral extends IntLiteral {
    public HexLiteral(TokenPosition tokenPosition, String hexLiteral) {
        super(tokenPosition, hexLiteral);
    }

    @Override
    public Long convertToLong() {
        return Long.parseLong(literal.substring(2), 16);
    }

    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }
}