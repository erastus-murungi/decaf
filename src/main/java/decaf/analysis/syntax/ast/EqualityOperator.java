package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;

import decaf.shared.env.Scope;

public class EqualityOperator extends BinOperator {

  public EqualityOperator(
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
    switch (label) {
      case Scanner.EQ:
        return "Eq";
      case Scanner.NEQ:
        return "NotEq";
      default:
        throw new IllegalArgumentException("please register equality operator: " + label);
    }
  }

  @Override
  public <ReturnType, InputType> ReturnType accept(
      AstVisitor<ReturnType, InputType> astVisitor,
      InputType input
  ) {
    return null;
  }

  @Override
  public String getSourceCode() {
    switch (label) {
      case Scanner.EQ:
        return "==";
      case Scanner.NEQ:
        return "!=";
      default:
        throw new IllegalArgumentException("please register equality operator: " + label);
    }
  }
}