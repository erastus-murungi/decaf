package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.lexical.Scanner;
import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class ImportDeclaration extends Declaration {
  public final RValue RValueId;

  public ImportDeclaration(RValue RValueId) {
    this.RValueId = RValueId;
  }

  @Override
  public Type getType() {
    return Type.Undefined;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(new Pair<>(
        "name",
        RValueId
    ));
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "ImportDeclaration{" + "nameId=" + RValueId + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
        "%s %s",
        Scanner.RESERVED_IMPORT,
        RValueId.getSourceCode()
    );
  }

  @Override
  public <T> T accept(
      AstVisitor<T> ASTVisitor,
      Scope curScope
  ) {
    return ASTVisitor.visit(
        this,
        curScope
    );
  }

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return null;
  }
}
