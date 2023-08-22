package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
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
    switch (label) {
      case Scanner.MINUS:
        return "Neg";
      case Scanner.NOT:
        return "Not";
      default:
        throw new IllegalArgumentException("please register unary operator: " + label);
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
    switch (label) {
      case Scanner.MINUS:
        return "-";
      case Scanner.NOT:
        return "!";
      default:
        throw new IllegalArgumentException("please register unary operator: " + label);
    }
  }

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return null;
  }
}