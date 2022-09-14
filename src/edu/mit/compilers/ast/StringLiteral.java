package edu.mit.compilers.ast;

import java.util.Collections;
import java.util.List;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.ASTVisitor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;

public class StringLiteral extends MethodCallParameter {
    final public String literal;
    final TokenPosition tokenPosition;

    public StringLiteral(TokenPosition tokenPosition, String literal) {
        this.tokenPosition = tokenPosition;
        this.literal = literal;
    }

    @Override
    public Type getType() {
        return Type.String;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    @Override
    public String toString() {
        return "StringLiteral{" + literal + "}";

    }

    @Override
    public String getSourceCode() {
        return literal;
    }

    @Override
    public <T> T accept(ASTVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }
}
