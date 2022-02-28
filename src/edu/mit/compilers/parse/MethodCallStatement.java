package edu.mit.compilers.parse;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class MethodCallStatement extends Statement {
    final MethodCall methodCall;

    public MethodCallStatement(TokenPosition tokenPosition, MethodCall methodCall) {
        super(tokenPosition);
        this.methodCall = methodCall;
    }

    @Override
    public List<Pair<String, Node>> getChildren() {
        return methodCall.getChildren();
    }

    @Override
    public String toString() {
        return methodCall.toString();
    }

    @Override
    public boolean isTerminal() {
        return false;
    }
}
