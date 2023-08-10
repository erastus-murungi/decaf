package decaf.ast;


import decaf.grammar.TokenPosition;

public abstract class Statement extends AST {
  public final TokenPosition tokenPosition;

  public Statement(TokenPosition tokenPosition) {
    this.tokenPosition = tokenPosition;
  }

  @Override
  public Type getType() {
    return Type.Undefined;
  }
}
