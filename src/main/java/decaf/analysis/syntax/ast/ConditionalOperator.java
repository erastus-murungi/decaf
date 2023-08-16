package decaf.analysis.syntax.ast;


import decaf.analysis.lexical.Scanner;
import decaf.analysis.TokenPosition;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.analysis.semantic.AstVisitor;
import decaf.shared.symboltable.SymbolTable;

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
    return switch (label) {
      case Scanner.CONDITIONAL_OR -> "Or";
      case Scanner.CONDITIONAL_AND -> "And";
      default -> throw new IllegalArgumentException("please register conditional operator: " + label);
    };
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
    return switch (label) {
      case Scanner.CONDITIONAL_OR -> "||";
      case Scanner.CONDITIONAL_AND -> "&&";
      default -> throw new IllegalArgumentException("please register conditional operator: " + label);
    };
  }

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return null;
  }
}