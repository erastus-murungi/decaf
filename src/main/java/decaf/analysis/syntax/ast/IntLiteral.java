package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.analysis.syntax.ast.types.Type;
import decaf.shared.AstVisitor;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

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
          getTokenPosition(),
          literal.substring(1)
      );
    } else {
      return new IntLiteral(
          getTokenPosition(),
          "-" + literal
      );
    }
  }

  public <ReturnType, InputType> ReturnType accept(
      AstVisitor<ReturnType, InputType> astVisitor,
      InputType input
  ) {
    return astVisitor.visit(
        this,
        input
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