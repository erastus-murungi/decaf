package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;
import decaf.shared.env.Scope;

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
    return switch (label) {
      case Scanner.ASSIGN -> "Assign";
      case Scanner.ADD_ASSIGN -> "AugmentedAdd";
      case Scanner.MINUS_ASSIGN -> "AugmentedSub";
      case Scanner.MULTIPLY_ASSIGN -> "AugmentedMul";
      default -> throw new IllegalArgumentException("please register assign operator: " + label);
    };
  }

  @Override
  public <T> T accept(AstVisitor<T> ASTVisitor, Scope currentScope) {
    throw new IllegalStateException("not meant to be called");
  }

  @Override
  public String getSourceCode() {
    return switch (label) {
      case Scanner.ASSIGN -> "=";
      case Scanner.ADD_ASSIGN -> "+=";
      case Scanner.MINUS_ASSIGN -> "-=";
      case Scanner.MULTIPLY_ASSIGN -> "*=";
      default -> throw new IllegalArgumentException("please register assign operator: " + label);
    };
  }
}
