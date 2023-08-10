package decaf.ast;


import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.grammar.Scanner;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

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
      SymbolTable curSymbolTable
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