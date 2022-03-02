package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;
import java.lang.reflect.*;

import java.util.ArrayList;
import java.util.List;

public class MethodDefinitionParameter extends AST {
    final Name id;
    final BuiltinType builtinType;

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("id", id), new Pair<>("type", builtinType));
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "id=" + id + ", type=" + builtinType + '}';
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    public MethodDefinitionParameter(Name id, BuiltinType builtinType) {
        this.id = id;
        this.builtinType = builtinType;
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this);
    }
}
