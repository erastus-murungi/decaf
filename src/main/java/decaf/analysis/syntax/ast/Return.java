package decaf.analysis.syntax.ast;


import java.util.Collections;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.Pair;
import decaf.shared.env.Scope;

public class Return extends Statement implements HasExpression {
  public Expression retExpression;

  public Return(
      TokenPosition tokenPosition,
      Expression expression
  ) {
    super(tokenPosition);
    this.retExpression = expression;
  }

  @Override
  public List<Pair<String, AST>> getChildren() {
    return (retExpression == null) ? Collections.emptyList(): List.of(new Pair<>(
        "return",
        retExpression
    ));
  }

  @Override
  public boolean isTerminal() {
    return retExpression == null;
  }

  @Override
  public String toString() {
    return (retExpression == null) ? "Return{}": "Return{" + "retExpression=" + retExpression + '}';
  }

  @Override
  public String getSourceCode() {
    return String.format(
        "%s %s",
        Scanner.RESERVED_RETURN,
        retExpression == null ? "": retExpression.getSourceCode()
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
    return codegenAstVisitor.visit(
        this,
        resultLocation
    );
  }

  @Override
  public List<Expression> getExpression() {
    if (retExpression == null)
      return Collections.emptyList();
    return List.of(retExpression);
  }

  @Override
  public void compareAndSwapExpression(
      Expression oldExpr,
      Expression newExpr
  ) {
    if (retExpression == oldExpr)
      retExpression = newExpr;
  }
}
