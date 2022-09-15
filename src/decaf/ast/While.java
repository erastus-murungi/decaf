package decaf.ast;

import java.util.List;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignableValue;
import decaf.common.Pair;
import decaf.common.Utils;
import decaf.grammar.DecafScanner;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class While extends Statement implements HasExpression {
    public final Block body;
    public Expression test;

    public While(TokenPosition tokenPosition, Expression test, Block body) {
        super(tokenPosition);
        this.test = test;
        this.body = body;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("test", test), new Pair<>("body", body));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "While{" + "test=" + test + ", body=" + body + '}';
    }

    @Override
    public String getSourceCode() {
        return String.format("%s (%s) {\n    %s\n    }", DecafScanner.RESERVED_WHILE, test.getSourceCode(), Utils.indentBlock(body));
    }

    @Override
    public <T> T accept(AstVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }


    @Override
    public List<Expression> getExpression() {
        return List.of(test);
    }

    @Override
    public void compareAndSwapExpression(Expression oldExpr, Expression newExpr) {
        if (test == oldExpr)
            test = newExpr;
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
        return null;
    }
}
