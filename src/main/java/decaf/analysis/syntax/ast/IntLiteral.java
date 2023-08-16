package decaf.analysis.syntax.ast;


import java.util.Objects;

import decaf.analysis.TokenPosition;
import decaf.analysis.semantic.AstVisitor;
import decaf.ir.CodegenAstVisitor;
import decaf.ir.names.IrAssignable;
import decaf.shared.env.Scope;

public class IntLiteral extends Literal {
  public IntLiteral(
      TokenPosition tokenPosition,
      String literalToken
  ) {
    super(
        tokenPosition,
        literalToken
    );
  }

  public Long convertToLong() {
    return Long.decode(
        literal
    );
  }

  public IntLiteral negate() {
    if (literal.startsWith("-")) {
      return new IntLiteral(
          tokenPosition,
          literal.substring(1)
      );
    } else {
      return new IntLiteral(
          tokenPosition,
          "-" + literal
      );
    }
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    IntLiteral intLiteral = (IntLiteral) o;
    return Objects.equals(
        convertToLong(),
        intLiteral.convertToLong()
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(convertToLong());
  }

  @Override
  public Type getType() {
    return Type.Int;
  }
}