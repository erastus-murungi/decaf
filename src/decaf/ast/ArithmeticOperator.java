package decaf.ast;


import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.grammar.DecafScanner;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class ArithmeticOperator extends BinOperator {
  public ArithmeticOperator(
      TokenPosition tokenPosition,
      @DecafScanner.ArithmeticOperator String op
  ) {
    super(
        tokenPosition,
        op
    );
  }

  @Override
  public String opRep() {
    return switch (label) {
      case DecafScanner.PLUS -> "Add";
      case DecafScanner.MINUS -> "Sub";
      case DecafScanner.MULTIPLY -> "Multiply";
      case DecafScanner.DIVIDE -> "Divide";
      case DecafScanner.MOD -> "Mod";
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
      SymbolTable curSymbolTable
  ) {
    return null;
  }

  @Override
  public String getSourceCode() {
    return switch (label) {
      case DecafScanner.PLUS -> "+";
      case DecafScanner.MINUS -> "-";
      case DecafScanner.MULTIPLY -> "*";
      case DecafScanner.DIVIDE -> "/";
      case DecafScanner.MOD -> "%";
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
