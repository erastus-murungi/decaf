package edu.mit.compilers.ast;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;

public class StringLiteral extends MethodCallParameter {
    final TokenPosition tokenPosition;
    final public String literal;

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
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }
}
