package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FieldDeclaration extends AST {
    final public TokenPosition tokenPosition;
    final public BuiltinType builtinType;
    final public List<Name> names;
    final public List<Array> arrays;

    public FieldDeclaration(TokenPosition tokenPosition, BuiltinType builtinType, List<Name> names, List<Array> arrays) {
        this.tokenPosition = tokenPosition;
        this.builtinType = builtinType;
        this.names = names;
        this.arrays = arrays;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        ArrayList<Pair<String, AST>> pairArrayList = new ArrayList<>();
        pairArrayList.add(new Pair<>("type", new Name(builtinType.toString(), tokenPosition, ExprContext.DECLARE)));
        for (Name name : names)
            pairArrayList.add(new Pair<>("var", name));
        for (Array array : arrays)
            pairArrayList.add(new Pair<>("array", array));
        return pairArrayList;
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "FieldDeclaration{" + "type=" + builtinType + ", names=" + names + ", arrays=" + arrays + '}';
    }

    @Override
    public String getSourceCode() {
        List<String> stringList = new ArrayList<>();
        for (Name name: names)
            stringList.add(name.getSourceCode());
        for (Array array: arrays)
            stringList.add(array.getSourceCode());
        String args = String.join(", ", stringList);
        return String.format("%s %s", builtinType.getSourceCode(), args);
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
      return visitor.visit(this, curSymbolTable);
    }
}
