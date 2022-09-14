package edu.mit.compilers.ast;

import static edu.mit.compilers.grammar.DecafScanner.RESERVED_LEN;

import java.util.List;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.grammar.TokenPosition;
import edu.mit.compilers.ir.ASTVisitor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;

public class Len extends Expression {
    final public Name nameId;
    final public Type type = Type.Int;

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
        return String.format("%s (%s)", RESERVED_LEN, nameId.getLabel());
    }

    @Override
    public <T> T accept(ASTVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
        return codegenAstVisitor.visit(this);
    }

}
