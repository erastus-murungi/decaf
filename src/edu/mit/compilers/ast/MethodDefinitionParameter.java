package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class MethodDefinitionParameter extends AST {
    final TokenPosition tokenPosition;
    final public Name id;
    final public BuiltinType builtinType;

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("id", id), new Pair<>("type", new Name(builtinType.toString(), tokenPosition, ExprContext.DECLARE)));
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "id=" + id + ", type=" + builtinType + '}';
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    public MethodDefinitionParameter(TokenPosition tokenPosition, Name id, BuiltinType builtinType) {
        this.tokenPosition = tokenPosition;
        this.id = id;
        this.builtinType = builtinType;
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }
}
