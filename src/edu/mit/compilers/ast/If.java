package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

import java.util.ArrayList;
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
        List<String> list = new ArrayList<>();
        for (String s1 : ifBlock.getSourceCode().split("\n")) {
            String s2 = "    " + s1;
            list.add(s2);
        }
        String indentedBlockString = String.join("\n", list);
        if ((elseBlock == null)) {
            return String.format("%s (%s) {\n    %s\n    }", RESERVED_IF, test.getSourceCode(), indentedBlockString);
        } else {
            List<String> result = new ArrayList<>();
            for (String s : elseBlock.getSourceCode().split("\n")) {
                String s1 = "    " + s;
                result.add(s1);
            }
            return String.format("%s (%s) {\n    %s\n    } %s {\n    %s        \n    }", RESERVED_IF, test.getSourceCode(), indentedBlockString, RESERVED_ELSE, String.join("\n", result));
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }
}
