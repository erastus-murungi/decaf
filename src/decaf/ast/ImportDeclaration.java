package decaf.ast;


import java.util.List;

import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.common.Pair;
import decaf.grammar.DecafScanner;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

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
        DecafScanner.RESERVED_IMPORT,
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
