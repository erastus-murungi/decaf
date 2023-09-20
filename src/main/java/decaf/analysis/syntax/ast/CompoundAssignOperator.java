package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;
import decaf.shared.env.Scope;

public class CompoundAssignOperator extends Operator {
  public CompoundAssignOperator(
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
      case Scanner.ADD_ASSIGN -> "AugmentedAdd";
      case Scanner.MINUS_ASSIGN -> "AugmentedSub";
      case Scanner.MULTIPLY_ASSIGN -> "AugmentedMul";
      default -> throw new IllegalArgumentException("please register compound assign operator: " + label);
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
    return switch (label) {
      case Scanner.ADD_ASSIGN -> "+=";
      case Scanner.MINUS_ASSIGN -> "-=";
      case Scanner.MULTIPLY_ASSIGN -> "*=";
      default -> throw new IllegalArgumentException("please register compound assign operator: " + label);
    };
  }
}