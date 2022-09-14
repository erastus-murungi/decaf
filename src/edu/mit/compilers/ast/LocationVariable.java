package edu.mit.compilers.ast;

import java.util.Collections;
import java.util.List;

import edu.mit.compilers.codegen.CodegenAstVisitor;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.ir.ASTVisitor;
import edu.mit.compilers.symboltable.SymbolTable;
import edu.mit.compilers.utils.Pair;

public class LocationVariable extends Location {
    public LocationVariable(Name name) {
        super(name);
    }

    @Override
    public List<Pair<String, AST>> getChildren() {
        return Collections.singletonList(new Pair<>("name", name));
    }

    @Override
    public <T> T accept(ASTVisitor<T> ASTVisitor, SymbolTable curSymbolTable) {
        return ASTVisitor.visit(this, curSymbolTable);
    }

    public <T> T accept(CodegenAstVisitor<T> codegenAstVisitor, IrAssignableValue resultLocation) {
        return codegenAstVisitor.visit(this, resultLocation);
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

    @Override
    public String toString() {
        return "LocationVariable{" + "name=" + name + '}';
    }

    @Override
    public String getSourceCode() {
        return name.getSourceCode();
    }
}
