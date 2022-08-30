package edu.mit.compilers.ast;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;


/* A variable name. id holds the name as a string, and ctx is one of the following types. (Load, Store) */
public class Name extends AST {
    private String label;
    public final TokenPosition tokenPosition;
    public final ExprContext context;

    public Name(String label, TokenPosition tokenPosition, ExprContext context) {
        this.label = label;
        this.tokenPosition = tokenPosition;
        this.context = context;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return "Name{" + "label='" + label + '\'' + ", context=" + context + '}';
    }

    @Override
    public String getSourceCode() {
        return label;
    }

    @Override
    public Type getType() {
        return Type.Undefined;
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
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }
}
