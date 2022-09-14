package edu.mit.compilers.ast;

import static edu.mit.compilers.grammar.DecafScanner.RESERVED_WHILE;

import java.util.List;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.ASTVisitor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;
import edu.mit.compilers.utils.Utils;

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
        return String.format("%s (%s) {\n    %s\n    }", RESERVED_WHILE, test.getSourceCode(), Utils.indentBlock(body));
    }

    @Override
    public <T> T accept(ASTVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
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
