package decaf.analysis.syntax.ast;


import java.util.Collections;
import java.util.List;

import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;

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
  public <ReturnType, InputType> ReturnType accept(
      AstVisitor<ReturnType, InputType> astVisitor,
      InputType input
  ) {
    return astVisitor.visit(
        this,
        input
    );
  }

  @Override
  public List<Expression> getExpressions() {
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
