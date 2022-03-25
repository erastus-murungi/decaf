package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.utils.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.mit.compilers.grammar.DecafScanner.RESERVED_FOR;

public class For extends Statement {
    public final Initialization initialization;

    final public Expression terminatingCondition;

    public final Update update;
    public final Block block;

    public For(TokenPosition tokenPosition,
               Initialization initialization,
               Expression terminatingCondition,
               Update update,
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
                new Pair<>("update", update));
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
        String blockString = block.getSourceCode();
        List<String> list = new ArrayList<>();
        for (String s : blockString.split("\n")) {
            String s1 = "    " + s;
            list.add(s1);
        }
        String indentedBlockString = String.join("\n", list);
        return String.format("%s (%s; %s; %s) {\n    %s\n    }",
                RESERVED_FOR, initialization.getSourceCode(), terminatingCondition.getSourceCode(), update.getSourceCode(), indentedBlockString);
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }



}
