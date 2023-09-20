package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.analysis.syntax.ast.types.Type;
import decaf.shared.AstVisitor;
import decaf.shared.env.Scope;
import org.jetbrains.annotations.NotNull;

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
  public @NotNull Type getType() {
    return Type.getBoolType();
  }
}
