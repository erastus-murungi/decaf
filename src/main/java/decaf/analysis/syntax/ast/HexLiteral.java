package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.env.Scope;


public class HexLiteral extends IntLiteral {
  public HexLiteral(
      TokenPosition tokenPosition,
      String hexLiteral
  ) {
    super(
        tokenPosition,
        hexLiteral
    );
  }

  @Override
  public Long convertToLong() {
    return Long.parseLong(
        literal.substring(2),
        16
    );
  }

  public <T> T accept(
      AstVisitor<T> ASTVisitor,
      Scope curScope
  ) {
    return ASTVisitor.visit(
        this,
        curScope
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