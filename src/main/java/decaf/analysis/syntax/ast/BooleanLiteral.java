package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.env.Scope;

public class BooleanLiteral extends IntLiteral {
  public BooleanLiteral(
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
    if (Boolean.parseBoolean(literal.translateEscapes())) {
      return 1L;
    } else {
      return 0L;
    }
  }

  @Override
  public String toString() {
    return "BooleanLiteral{" + "literal='" + literal + '\'' + '}';
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
  public Type getType() {
    return Type.Bool;
  }

}
