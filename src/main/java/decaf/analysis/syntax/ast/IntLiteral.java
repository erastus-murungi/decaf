package decaf.analysis.syntax.ast;


import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import decaf.analysis.TokenPosition;
import decaf.shared.AstVisitor;

import decaf.shared.env.Scope;

public class IntLiteral extends Literal implements Typed<IntLiteral> {
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
  public @NotNull Type getType() {
    return Type.getIntType();
  }

  @Override
  public IntLiteral setType(@NotNull Type type) {
    throw new UnsupportedOperationException();
  }
}