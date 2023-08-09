package decaf.ast;


import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.grammar.DecafScanner;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class AssignOperator extends Operator {
  public AssignOperator(
      TokenPosition tokenPosition,
      @DecafScanner.AssignOperator String op
  ) {
    super(
        tokenPosition,
        op
    );
  }

  @Override
  public String opRep() {
    switch (label) {
      case DecafScanner.ASSIGN:
        return "Assign";
      case DecafScanner.ADD_ASSIGN:
        return "AugmentedAdd";
      case DecafScanner.MINUS_ASSIGN:
        return "AugmentedSub";
      case DecafScanner.MULTIPLY_ASSIGN:
        return "AugmentedMul";
      default:
        throw new IllegalArgumentException("please register assign operator: " + label);
    }
  }

  @Override
  public Type getType() {
    return Type.Undefined;
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
      case DecafScanner.ASSIGN:
        return "=";
      case DecafScanner.ADD_ASSIGN:
        return "+=";
      case DecafScanner.MINUS_ASSIGN:
        return "-=";
      case DecafScanner.MULTIPLY_ASSIGN:
        return "*=";
      default:
        throw new IllegalArgumentException("please register assign operator: " + label);
    }
  }

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return null;
  }
}
