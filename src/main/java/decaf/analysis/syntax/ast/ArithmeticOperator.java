package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.analysis.lexical.Scanner;
import decaf.shared.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.env.Scope;

public class ArithmeticOperator extends BinOperator {
  public ArithmeticOperator(
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
      case Scanner.PLUS -> "Add";
      case Scanner.MINUS -> "Sub";
      case Scanner.MULTIPLY -> "Multiply";
      case Scanner.DIVIDE -> "Divide";
      case Scanner.MOD -> "Mod";
      default -> throw new IllegalArgumentException("please register a display string for: " + label);
    };
  }

  @Override
  public Type getType() {
    return Type.Undefined;
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
      case Scanner.PLUS -> "+";
      case Scanner.MINUS -> "-";
      case Scanner.MULTIPLY -> "*";
      case Scanner.DIVIDE -> "/";
      case Scanner.MOD -> "%";
      default -> throw new IllegalArgumentException("please register a display string for: " + label);
    };
  }

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return null;
  }
}
