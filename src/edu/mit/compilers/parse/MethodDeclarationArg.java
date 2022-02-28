package edu.mit.compilers.parse;

import edu.mit.compilers.utils.Pair;

import java.util.List;

public class MethodDeclarationArg extends Node {
    final Name nameId;
    final BuiltinType builtinType;

    @Override
    public List<Pair<String, Node>> getChildren() {
        return List.of(new Pair<>("id", nameId), new Pair<>("type", builtinType));
    }

    @Override
    public String toString() {
        return "MethodDeclarationArg{" + "nameId=" + nameId + ", type=" + builtinType + '}';
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    public MethodDeclarationArg(Name nameId, BuiltinType builtinType) {
        this.nameId = nameId;
        this.builtinType = builtinType;
    }
}
