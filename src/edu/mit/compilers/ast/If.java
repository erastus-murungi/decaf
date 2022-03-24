package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

import java.util.Arrays;
import java.util.List;

import static edu.mit.compilers.grammar.DecafScanner.RESERVED_ELSE;
import static edu.mit.compilers.grammar.DecafScanner.RESERVED_IF;

public class If extends Statement {
    public final Expression test;
    public final Block ifBlock;
    public final Block elseBlock; // maybe null

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
        String indentedBlockString = String.join("\n", Arrays.stream(ifBlock.getSourceCode().split("\n")).map(s -> "    " + s).toList());
        return (elseBlock == null) ?
                String.format("%s (%s) {\n    %s\n    }", RESERVED_IF, test.getSourceCode(), indentedBlockString) :
                String.format("%s (%s) {\n    %s\n    } %s {\n    %s        \n    }", RESERVED_IF, test.getSourceCode(), indentedBlockString, RESERVED_ELSE, String.join("\n", Arrays.stream(elseBlock.getSourceCode().split("\n")).map(s -> "    " + s).toList()));
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }
}
