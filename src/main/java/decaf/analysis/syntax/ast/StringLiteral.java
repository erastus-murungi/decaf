package decaf.analysis.syntax.ast;


import java.util.Collections;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class StringLiteral extends ActualArgument {
  final public String literal;
  final TokenPosition tokenPosition;

  public StringLiteral(
      TokenPosition tokenPosition,
      String literal
  ) {
    this.tokenPosition = tokenPosition;
    this.literal = literal;
  }

  @Override
  public Type getType() {
    return Type.String;
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
    return "StringLiteral{" + literal + "}";
  }

  @Override
  public String getSourceCode() {
    return literal;
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
    return codegenAstVisitor.visit(
        this,
        resultLocation
    );
  }
}
