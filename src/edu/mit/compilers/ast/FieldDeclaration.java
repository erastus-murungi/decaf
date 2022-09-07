package edu.mit.compilers.ast;

import java.util.ArrayList;
import java.util.List;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.ASTVisitor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;

public class FieldDeclaration extends Declaration {
    final public TokenPosition tokenPosition;
    final public List<Name> names;
    final public List<Array> arrays;
    final private Type type;

    public FieldDeclaration(TokenPosition tokenPosition, Type type, List<Name> names, List<Array> arrays) {
        this.tokenPosition = tokenPosition;
        this.type = type;
        this.names = names;
        this.arrays = arrays;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        ArrayList<Pair<String, AST>> pairArrayList = new ArrayList<>();
        pairArrayList.add(new Pair<>("type", new Name(type.toString(), tokenPosition, ExprContext.DECLARE)));
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
        return "FieldDeclaration{" + "type=" + type + ", names=" + names + ", arrays=" + arrays + '}';
    }

    @Override
    public String getSourceCode() {
        List<String> stringList = new ArrayList<>();
        for (Name name : names)
            stringList.add(name.getSourceCode());
        for (Array array : arrays)
            stringList.add(array.getSourceCode());
        String args = String.join(", ", stringList);
        return String.format("%s %s", type.getSourceCode(), args);
    }

    @Override
    public <T> T accept(ASTVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, LValue resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }

    @Override
    public Type getType() {
        return type;
    }
}
