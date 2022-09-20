package decaf.ast;

import java.util.Collections;
import java.util.List;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.codegen.names.IrAssignable;
import decaf.common.Pair;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class Array extends AST {
    private final IntLiteral size;
    private final Name id;

    public Array(IntLiteral size, Name id) {
        this.size = size;
        this.id = id;
    }

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

    @Override
    public String toString() {
        return "Array{" + "size=" + size + ", id=" + id + '}';
    }

    @Override
    public String getSourceCode() {
        return String.format("%s[%s]", id.getLabel(), size.literal);
    }

    @Override
    public <T> T accept(AstVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignable resultLocation) {
        return null;
    }
}
