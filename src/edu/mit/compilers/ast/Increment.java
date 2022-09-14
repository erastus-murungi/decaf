package edu.mit.compilers.ast;


import static edu.mit.compilers.grammar.DecafScanner.INCREMENT;

import java.util.Collections;
import java.util.List;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.ASTVisitor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;

public class Increment extends AssignExpr {
    public Increment(TokenPosition tokenPosition) {
        super(tokenPosition, null);
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
        return "Increment{}";
    }

    @Override
    public String getSourceCode() {
        return INCREMENT;
    }

    @Override
    public <T> T accept(ASTVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }

    @Override
    public String getOperator() {
        return "++";
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
        return null;
    }
}
