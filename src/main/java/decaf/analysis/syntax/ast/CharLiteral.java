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
  public <T> T accept(
      AstVisitor<T> astVisitor,
      Scope curScope
  ) {
    return astVisitor.visit(
        this,
        curScope
    );
  }
}
