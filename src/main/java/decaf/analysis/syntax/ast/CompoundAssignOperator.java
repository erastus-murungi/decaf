package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
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
    switch (label) {
      case Scanner.ADD_ASSIGN:
        return "AugmentedAdd";
      case Scanner.MINUS_ASSIGN:
        return "AugmentedSub";
      case Scanner.MULTIPLY_ASSIGN:
        return "AugmentedMul";
      default:
        throw new IllegalArgumentException("please register compound assign operator: " + label);
    }
  }

  @Override
  public <T> T accept(
      AstVisitor<T> ASTVisitor,
      Scope curScope
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

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return null;
  }
}