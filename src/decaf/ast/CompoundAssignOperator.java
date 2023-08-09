package decaf.ast;


import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.grammar.DecafScanner;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class CompoundAssignOperator extends Operator {
  public CompoundAssignOperator(
      TokenPosition tokenPosition,
      @DecafScanner.CompoundAssignOperator String op
  ) {
    super(
        tokenPosition,
        op
    );
  }

  @Override
  public String opRep() {
    switch (label) {
      case DecafScanner.ADD_ASSIGN:
        return "AugmentedAdd";
      case DecafScanner.MINUS_ASSIGN:
        return "AugmentedSub";
      case DecafScanner.MULTIPLY_ASSIGN:
        return "AugmentedMul";
      default:
        throw new IllegalArgumentException("please register compound assign operator: " + label);
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
    return switch (label) {
      case DecafScanner.ADD_ASSIGN -> "+=";
      case DecafScanner.MINUS_ASSIGN -> "-=";
      case DecafScanner.MULTIPLY_ASSIGN -> "*=";
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