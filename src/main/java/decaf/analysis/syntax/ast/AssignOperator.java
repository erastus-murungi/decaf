package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;

public class AssignOperator extends Operator {
  public AssignOperator(
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
      case Scanner.ASSIGN -> "Assign";
      case Scanner.ADD_ASSIGN -> "AugmentedAdd";
      case Scanner.MINUS_ASSIGN -> "AugmentedSub";
      case Scanner.MULTIPLY_ASSIGN -> "AugmentedMul";
      default -> throw new IllegalArgumentException("please register assign operator: " + getLabel());
    };
  }

  @Override
  public <ReturnType, InputType> ReturnType accept(AstVisitor<ReturnType, InputType> astVisitor, InputType input) {
    throw new IllegalStateException("not meant to be called");
  }

  @Override
  public String getSourceCode() {
    return switch (getLabel()) {
      case Scanner.ASSIGN -> "=";
      case Scanner.ADD_ASSIGN -> "+=";
      case Scanner.MINUS_ASSIGN -> "-=";
      case Scanner.MULTIPLY_ASSIGN -> "*=";
      default -> throw new IllegalArgumentException("please register assign operator: " + getLabel());
    };
  }
}
