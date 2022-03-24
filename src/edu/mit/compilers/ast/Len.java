package edu.mit.compilers.ast;

import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.Visitor;
import edu.mit.compilers.symbolTable.SymbolTable;
import edu.mit.compilers.utils.Pair;

import java.util.List;

import static edu.mit.compilers.grammar.DecafScanner.RESERVED_LEN;

public class Len extends Expression {
    final public Name nameId;
    final public BuiltinType builtinType = BuiltinType.Int;

    public Len(TokenPosition tokenPosition, Name nameId) {
        super(tokenPosition);
        this.tokenPosition = tokenPosition;
        this.nameId = nameId;
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return List.of(new Pair<>("id", nameId));
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public String toString() {
        return "Len{" + "nameId=" + nameId + '}';
    }

    @Override
    public String getSourceCode() {
        return String.format("%s (%s)", RESERVED_LEN, nameId.id);
    }

    @Override
    public <T> T accept(Visitor<T> visitor, SymbolTable curSymbolTable) {
        return visitor.visit(this, curSymbolTable);
    }
}
