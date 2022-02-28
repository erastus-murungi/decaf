package edu.mit.compilers.parse;

import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;

public class BreakOrContinue extends Statement {
    final Type type;

    public enum Type {
        BREAK, CONTINUE
    }

    public BreakOrContinue(TokenPosition position, Type type) {
        super(position);
        this.type = type;
    }

    @Override
    public List<Pair<String, Node>> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    @Override
    public String toString() {
        return (type == Type.BREAK) ? DecafScanner.RESERVED_BREAK : DecafScanner.RESERVED_CONTINUE;
    }
}
