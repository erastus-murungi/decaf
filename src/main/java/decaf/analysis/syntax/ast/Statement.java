package decaf.analysis.syntax.ast;


import decaf.analysis.TokenPosition;

public abstract class Statement extends AST {
  public final TokenPosition tokenPosition;

  public Statement(TokenPosition tokenPosition) {
      super(tokenPosition);
      this.tokenPosition = tokenPosition;
  }
}
