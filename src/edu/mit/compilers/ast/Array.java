package edu.mit.compilers.ast;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;

public class Array extends AST {
    private final IntLiteral size;
    private final Name id;

    public IntLiteral getSize() {
        return size;
    }

    public Name getId() {
        return id;
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

    public Array(IntLiteral size, Name id) {
        this.size = size;
        this.id = id;
    }

    @Override
    public String toString() {
        return "Array{" + "size=" + size + ", id=" + id + '}';
    }

    @Override
    public String getSourceCode() {
        return String.format("%s[%s]", id.getLabel(), size.literal);
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return null;
    }
}
