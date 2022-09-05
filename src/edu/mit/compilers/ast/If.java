package edu.mit.compilers.ast;

import static edu.mit.compilers.grammar.DecafScanner.RESERVED_ELSE;
import static edu.mit.compilers.grammar.DecafScanner.RESERVED_IF;

import java.util.List;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;
import edu.mit.compilers.utils.Utils;

public class If extends Statement implements HasExpression {
    public final Block ifBlock;
    public final Block elseBlock; // maybe null
    public Expression test;

    public If(TokenPosition tokenPosition, Expression test, Block ifBlock, Block elseBlock) {
        super(tokenPosition);
        this.test = test;
        this.ifBlock = ifBlock;
        this.elseBlock = elseBlock;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return (elseBlock != null)
                ? List.of(
                new Pair<>("test", test),
                new Pair<>("ifBody", ifBlock),
                new Pair<>("elseBody", elseBlock))
                : List.of(new Pair<>("test", test), new Pair<>("ifBody", ifBlock));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        if (elseBlock != null)
            return "If{" + "test=" + test + ", ifBlock=" + ifBlock + ", elseBlock=" + elseBlock + '}';
        else return "If{" + "test=" + test + ", ifBlock=" + ifBlock + '}';
    }

    @Override
    public String getSourceCode() {
        String indentedBlockString = Utils.indentBlock(ifBlock);
        if ((elseBlock == null)) {
            return String.format("%s (%s) {\n    %s\n    }", RESERVED_IF, test.getSourceCode(), indentedBlockString);
        } else {
            return String.format("%s (%s) {\n    %s\n    } %s {\n    %s        \n    }", RESERVED_IF, test.getSourceCode(), indentedBlockString, RESERVED_ELSE, Utils.indentBlock(elseBlock));
        }
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

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return null;
    }
}
