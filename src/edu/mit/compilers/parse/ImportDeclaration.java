package edu.mit.compilers.parse;

import edu.mit.compilers.utils.Pair;

import java.util.List;

public class ImportDeclaration extends Node {
    final Name nameId;

    public ImportDeclaration(Name nameId) {
        this.nameId = nameId;
    }

    @Override
    public List<Pair<String, Node>> getChildren() {
        return List.of(new Pair<>("name", nameId));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "ImportDeclaration{" + "nameId=" + nameId + '}';
    }
}
