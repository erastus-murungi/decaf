package edu.mit.compilers.ast;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.utils.Pair;
import edu.mit.compilers.utils.Utils;

import java.util.List;

import static edu.mit.compilers.grammar.DecafScanner.RESERVED_FOR;

public class For extends Statement implements HasExpression  {
    public final Initialization initialization;
    public Expression terminatingCondition;
    public final Assignment update;
    public final Block block;

    public For(TokenPosition tokenPosition,
               Initialization initialization,
               Expression terminatingCondition,
               Assignment update,
               Block block) {
        super(tokenPosition);
        this.initialization = initialization;
        this.terminatingCondition = terminatingCondition;
        this.update = update;
        this.block = block;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(
                new Pair<>("initialization", initialization),
                new Pair<>("terminatingCondition", terminatingCondition),
                new Pair<>("update", update),
                new Pair<>("block", block));
    }

    @Override
    public String toString() {
        return "For{" +
                "initialization=" + initialization +
                ", terminatingCondition=" + terminatingCondition +
                ", update=" + update +
                ", block=" + block +
                '}';
    }

    @Override
    public String getSourceCode() {
        return String.format("%s (%s; %s; %s) {\n    %s\n    }",
                RESERVED_FOR, initialization.getSourceCode(), terminatingCondition.getSourceCode(), update.getSourceCode(), Utils.indentBlock(block));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }


    @Override
    public List<Expression> getExpression() {
        return List.of(terminatingCondition);
    }

    @Override
    public void compareAndSwapExpression(Expression oldExpr, Expression newExpr) {
        if (terminatingCondition == oldExpr)
            terminatingCondition = newExpr;
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return null;
    }
}
