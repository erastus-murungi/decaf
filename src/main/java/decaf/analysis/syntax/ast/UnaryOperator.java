package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;

import decaf.shared.env.Scope;

public class UnaryOperator extends Operator {
  public UnaryOperator(
      TokenPosition tokenPosition,
      String op
  ) {
    super(
        tokenPosition,
        op
    );
  }

  @Override
  public String opRep() {
    return switch (label) {
      case Scanner.MINUS -> "Neg";
      case Scanner.NOT -> "Not";
      default -> throw new IllegalArgumentException("please register unary operator: " + label);
    };
  }

  @Override
  public <T> T accept(
      AstVisitor<T> astVisitor,
      Scope curScope
  ) {
    return null;
  }

  @Override
  public String getSourceCode() {
    switch (label) {
      case Scanner.MINUS:
        return "-";
      case Scanner.NOT:
        return "!";
      default:
        throw new IllegalArgumentException("please register unary operator: " + label);
    }
  }
}