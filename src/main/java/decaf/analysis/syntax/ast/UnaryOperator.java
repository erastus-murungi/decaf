package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;

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
    return switch (getLabel()) {
      case Scanner.MINUS -> "Neg";
      case Scanner.NOT -> "Not";
      default -> throw new IllegalArgumentException("please register unary operator: " + getLabel());
    };
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
      return switch (getLabel()) {
          case Scanner.MINUS -> "-";
          case Scanner.NOT -> "!";
          default -> throw new IllegalArgumentException("please register unary operator: " + getLabel());
      };
  }
}