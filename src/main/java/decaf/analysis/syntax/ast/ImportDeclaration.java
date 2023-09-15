package decaf.analysis.syntax.ast;


import java.util.List;

import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;

import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class ImportDeclaration extends Declaration {
  public final RValue value;

  public ImportDeclaration(RValue RValueId) {
    this.value = RValueId;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return List.of(new Pair<>(
        "name",
        value
    ));
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public String toString() {
    return "ImportDeclaration{" + "nameId=" + value + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
        "%s %s",
        Scanner.RESERVED_IMPORT,
        value.getSourceCode()
    );
  }

  @Override
  public <T> T accept(
      AstVisitor<T> astVisitor,
      Scope curScope
  ) {
    return astVisitor.visit(
        this,
        curScope
    );
  }
}
