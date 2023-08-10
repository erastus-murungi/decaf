package decaf.ast;

import decaf.grammar.TokenPosition;

public abstract class Expression extends AST {
  public TokenPosition tokenPosition;

  protected Type type;

  public Expression(TokenPosition tokenPosition) {
    this.tokenPosition = tokenPosition;
  }

  @Override
  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }
}
