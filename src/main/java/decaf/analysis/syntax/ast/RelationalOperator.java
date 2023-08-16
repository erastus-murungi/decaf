package decaf.analysis.syntax.ast;


import decaf.analysis.lexical.Scanner;
import decaf.analysis.TokenPosition;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.analysis.semantic.AstVisitor;
import decaf.shared.symboltable.SymbolTable;

public class RelationalOperator extends BinOperator {
  public RelationalOperator(
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
      case Scanner.LT:
        return "Lt";
      case Scanner.GT:
        return "Gt";
      case Scanner.GEQ:
        return "GtE";
      case Scanner.LEQ:
        return "LtE";
      default:
        throw new IllegalArgumentException("please register relational operator: " + label);
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
      case Scanner.LT:
        return "<";
      case Scanner.GT:
        return ">";
      case Scanner.GEQ:
        return ">=";
      case Scanner.LEQ:
        return "<=";
      default:
        throw new IllegalArgumentException("please register relational operator: " + label);
    }
  }

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return null;
  }
}