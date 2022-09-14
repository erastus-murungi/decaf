package edu.mit.compilers.ast;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.ASTVisitor;
import edu.mit.compilers.symboltable.SymbolTable;

public class BooleanLiteral extends IntLiteral {
    public BooleanLiteral(TokenPosition tokenPosition, @DecafScanner.BooleanLiteral String literal) {
        super(tokenPosition, literal);
    }

    @Override
    public Long convertToLong() {
        if (Boolean.parseBoolean(literal.translateEscapes())) {
            return 1L;
        } else {
            return 0L;
        }
    }

    @Override
    public String toString() {
        return "BooleanLiteral{" + "literal='" + literal + '\'' + '}';
    }

    @Override
    public <T> T accept(ASTVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }

    @Override
    public Type getType() {
        return Type.Bool;
    }

}
