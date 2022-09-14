package edu.mit.compilers.ast;

import static edu.mit.compilers.grammar.DecafScanner.RESERVED_IMPORT;

import java.util.List;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.ir.ASTVisitor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;

public class ImportDeclaration extends Declaration {
    public final Name nameId;

    public ImportDeclaration(Name nameId) {
        this.nameId = nameId;
    }

    @Override
    public Type getType() {
        return Type.Undefined;
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
    public String getSourceCode() {
        return String.format("%s %s", RESERVED_IMPORT, nameId.getSourceCode());
    }

    @Override
    public <T> T accept(ASTVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
        return null;
    }
}
