package edu.mit.compilers.ast;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class MethodDefinitionParameter extends Declaration {
    final public TokenPosition tokenPosition;
    final private String name;
    final private Type type;

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

    public MethodDefinitionParameter(TokenPosition tokenPosition, String name, Type type) {
        this.tokenPosition = tokenPosition;
        this.name = name;
        this.type = type;
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, AssignableName resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }
}
