package decaf.ast;


import decaf.codegen.CodegenAstVisitor;
import decaf.codegen.names.IrAssignable;
import decaf.grammar.TokenPosition;
import decaf.ir.AstVisitor;
import decaf.symboltable.SymbolTable;

public class CharLiteral extends IntLiteral {
  public CharLiteral(
      TokenPosition tokenPosition,
      String literal
  ) {
    super(
        tokenPosition,
        literal
    );
  }

  @Override
  public Long convertToLong() {
    return (long) literal.charAt(1);
  }

  @Override
  public <T> T accept(
      AstVisitor<T> ASTVisitor,
      SymbolTable curSymbolTable
  ) {
    return ASTVisitor.visit(
        this,
        curSymbolTable
    );
  }

  public <T> T accept(
      CodegenAstVisitor<T> codegenAstVisitor,
      IrAssignable resultLocation
  ) {
    return codegenAstVisitor.visit(
        this,
        resultLocation
    );
  }
}
