package edu.mit.compilers.parse;

import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;

public class Array extends Node {
    final IntLiteral size;
    final Name nameId;

    @Override
    public List<Pair<String, Node>> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    public Array(IntLiteral size, Name nameId) {
        this.size = size;
        this.nameId = nameId;
    }

    @Override
    public String toString() {
        return "Array{" + "size=" + size + ", nameId=" + nameId + '}';
    }
}
