package decaf.exceptions;


import decaf.grammar.TokenPosition;

public class DecafSemanticException extends DecafException {
  public DecafSemanticException(
      TokenPosition tokenPosition,
      String errMessage
  ) {
    super(
        tokenPosition,
        errMessage
    );
  }
}
