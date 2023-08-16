package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.lexical.Scanner;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.analysis.semantic.AstVisitor;
import decaf.shared.symboltable.SymbolTable;

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
    return List.of(new Pair<>(
        "name",
        nameId
    ));
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
    return String.format(
        "%s %s",
        Scanner.RESERVED_IMPORT,
        nameId.getSourceCode()
    );
  }

  @Override
  public <T> T accept(
      AstVisitor<T> ASTVisitor,
      SymbolTable curSymbolTable
  ) {
    return ASTVisitor.visit(
        this,
        curSymbolTable
    );
  }

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return null;
  }
}
