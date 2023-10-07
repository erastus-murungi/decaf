package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;

public class ConditionalOperator extends BinOperator {
  public ConditionalOperator(
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
      case Scanner.CONDITIONAL_OR -> "Or";
      case Scanner.CONDITIONAL_AND -> "And";
      default -> throw new IllegalArgumentException("please register conditional operator: " + getLabel());
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
      case Scanner.CONDITIONAL_OR -> "||";
      case Scanner.CONDITIONAL_AND -> "&&";
      default -> throw new IllegalArgumentException("please register conditional operator: " + getLabel());
    };
  }
}