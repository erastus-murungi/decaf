package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;

public class RelationalOperator extends BinOperator {
  public RelationalOperator(
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
    switch (getLabel()) {
      case Scanner.LT:
        return "Lt";
      case Scanner.GT:
        return "Gt";
      case Scanner.GEQ:
        return "GtE";
      case Scanner.LEQ:
        return "LtE";
      default:
        throw new IllegalArgumentException("please register relational operator: " + getLabel());
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
    switch (getLabel()) {
      case Scanner.LT:
        return "<";
      case Scanner.GT:
        return ">";
      case Scanner.GEQ:
        return ">=";
      case Scanner.LEQ:
        return "<=";
      default:
        throw new IllegalArgumentException("please register relational operator: " + getLabel());
    }
  }
}