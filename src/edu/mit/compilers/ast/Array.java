package edu.mit.compilers.ast;

import edu.mit.compilers.utils.Pair;

import java.util.Collections;
import java.util.List;

public class Array extends AST {
    final IntLiteral size;
    final Name id;

    @Override
    public List<Pair<String, AST>> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    public Array(IntLiteral size, Name id) {
        this.size = size;
        this.id = id;
    }

    @Override
    public String toString() {
        return "Array{" + "size=" + size + ", id=" + id + '}';
    }
}