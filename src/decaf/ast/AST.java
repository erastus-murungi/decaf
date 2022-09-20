package decaf.ast;

import java.util.List;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.common.Pair;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public abstract class AST {
    public abstract Type getType();

    public abstract List<Pair<String, AST>> getChildren();

    public abstract boolean isTerminal();

    public abstract <T> T accept(AstVisitor<T> ASTVisitor, SymbolTable currentSymbolTable);

    public abstract <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignable resultLocation);

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    public abstract String getSourceCode();
}
