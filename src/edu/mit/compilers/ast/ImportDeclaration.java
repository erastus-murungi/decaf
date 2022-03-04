package edu.mit.compilers.ast;

import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable; 
import edu.mit.compilers.descriptors.Descriptor;
import edu.mit.compilers.utils.Pair;

import java.util.List;

public class ImportDeclaration extends AST {
    public final Name nameId;

    public ImportDeclaration(Name nameId) {
        this.nameId = nameId;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
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

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }
}
