package edu.mit.compilers.parse;

import edu.mit.compilers.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class Block extends Node {
    final List<FieldDeclaration> fieldDeclarationList;
    final List<Statement> statementList;

    public Block(List<FieldDeclaration> fieldDeclarationList, List<Statement> statementList) {
        this.fieldDeclarationList = fieldDeclarationList;
        this.statementList = statementList;
    }

    @Override
    public List<Pair<String, Node>> getChildren() {
        ArrayList<Pair<String, Node>> nodeArrayList = new ArrayList<>();
        for (FieldDeclaration fieldDeclaration : fieldDeclarationList)
            nodeArrayList.add(new Pair<>("fieldDeclaration", fieldDeclaration));
        for (Statement statement : statementList)
            nodeArrayList.add(new Pair<>("statement", statement));
        return nodeArrayList;
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "Block{" + "fieldDeclarationList=" + fieldDeclarationList + ", statementList=" + statementList + '}';
    }
}
