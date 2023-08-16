package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.env.Scope;

public class DecimalLiteral extends IntLiteral {
  public DecimalLiteral(
      TokenPosition tokenPosition,
      String literalToken
  ) {
    super(
        tokenPosition,
        literalToken
    );
  }

  @Override
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

  @Override
  public Long convertToLong() {
    return Long.parseLong(literal);
  }
}