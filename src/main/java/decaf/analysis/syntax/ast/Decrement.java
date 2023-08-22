package decaf.analysis.syntax.ast;


import java.util.Collections;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class Decrement extends AssignExpr {
  public Decrement(TokenPosition tokenPosition) {
    super(
        tokenPosition,
        null
    );
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return Collections.emptyList();
  }

  @Override
  public boolean isTerminal() {
    return true;
  }

  @Override
  public String toString() {
    return "Decrement{}";
  }

  @Override
  public String getSourceCode() {
    return Scanner.DECREMENT;
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

  @Override
  public String getOperator() {
    return "--";
  }

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return null;
  }
}
