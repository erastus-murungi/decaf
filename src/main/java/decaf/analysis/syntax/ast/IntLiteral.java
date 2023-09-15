package decaf.analysis.syntax.ast;


import decaf.analysis.syntax.ast.types.Type;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import decaf.analysis.TokenPosition;
import decaf.shared.AstVisitor;

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
      AstVisitor<T> astVisitor,
      Scope curScope
  ) {
    return astVisitor.visit(
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
  public void setType(@NotNull Type type) {
    // do nothing;
    // literal types are constant
  }
}