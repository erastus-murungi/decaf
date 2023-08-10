package decaf.ast;


import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.grammar.Scanner;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

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