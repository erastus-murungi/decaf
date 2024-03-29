package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;
import decaf.shared.AstVisitor;

import decaf.shared.env.Scope;

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
  public <ReturnType, InputType> ReturnType accept(
      AstVisitor<ReturnType, InputType> astVisitor,
      InputType input
  ) {
    return astVisitor.visit(
        this,
        input
    );
  }
}
