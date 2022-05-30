package edu.mit.compilers.ast;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;
import edu.mit.compilers.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static edu.mit.compilers.grammar.DecafScanner.RESERVED_WHILE;

public class While extends Statement implements HasExpression  {
    public Expression test;
    public final Block body;

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
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
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

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, AssignableName resultLocation) {
        return null;
    }
}
