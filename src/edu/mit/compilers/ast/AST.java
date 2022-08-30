package edu.mit.compilers.ast;

import java.util.List;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

public abstract class AST {
    public abstract Type getType();

    public abstract List<Pair<String, AST>> getChildren();

    public abstract boolean isTerminal();

    public abstract <T> T accept(Visitor<T> visitor, SymbolTable currentSymbolTable);

    public abstract <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation);

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }

    public abstract String getSourceCode();
}
