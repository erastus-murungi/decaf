package decaf.ast;

import java.util.List;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignableValue;
import decaf.common.Pair;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

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
    public <T> T accept(AstVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }
}
