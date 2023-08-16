package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;

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
