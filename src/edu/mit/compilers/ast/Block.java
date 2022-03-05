package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class Block extends AST {
    final public List<FieldDeclaration> fieldDeclarationList;
    final public List<Statement> statementList;
    public SymbolTable blockSymbolTable;

    public Block(List<FieldDeclaration> fieldDeclarationList, List<Statement> statementList) {
        this.fieldDeclarationList = fieldDeclarationList;
        this.statementList = statementList;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        ArrayList<Pair<String, AST>> nodeArrayList = new ArrayList<>();
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

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }
}
