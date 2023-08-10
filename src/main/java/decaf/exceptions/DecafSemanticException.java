package decaf.exceptions;


import decaf.grammar.TokenPosition;

public class DecafSemanticException extends DecafScannerException {
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
