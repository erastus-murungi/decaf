package edu.mit.compilers.ast;

import java.util.List;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.ASTVisitor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;

public class MethodCallStatement extends Statement {
    public final MethodCall methodCall;

    public MethodCallStatement(TokenPosition tokenPosition, MethodCall methodCall) {
        super(tokenPosition);
        this.methodCall = methodCall;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return methodCall.getChildren();
    }

    @Override
    public String toString() {
        return methodCall.toString();
    }

    @Override
    public String getSourceCode() {
        return methodCall.getSourceCode();
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
