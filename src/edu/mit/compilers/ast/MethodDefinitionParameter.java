package edu.mit.compilers.ast;

import java.util.List;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.ASTVisitor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;

public class MethodDefinitionParameter extends Declaration {
    final public TokenPosition tokenPosition;
    final private String name;
    final private Type type;

    public MethodDefinitionParameter(TokenPosition tokenPosition, String name, Type type) {
        this.tokenPosition = tokenPosition;
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("type", new Name(type.toString(), tokenPosition, ExprContext.DECLARE)));
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "name=" + name + ", type=" + type + '}';
    }

    @Override
    public String getSourceCode() {
        return String.format("%s %s", type.getSourceCode(), name);
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public <T> T accept(ASTVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }
}
