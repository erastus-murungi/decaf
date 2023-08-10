package decaf.grammar;

public class ScannerError extends Token {
  private final ErrorType errorType;

  public ErrorType getErrorType() {
    return errorType;
  }

  public ScannerError(
      TokenPosition tokenPosition,
      ErrorType errorType,
      String detail
  ) {
    super(tokenPosition, TokenType.ERROR, detail);
    this.errorType = errorType;
  }

  public String getErrorSummary() {
    return switch (errorType) {
      case INVALID_CHAR -> "invalid character";
      case INVALID_CHAR_LITERAL -> "invalid char literal";
      case INVALID_COMMENT -> "invalid comment";
      case INVALID_ESCAPE_SEQUENCE -> "invalid escape sequence";
      case INVALID_WHITESPACE -> "invalid whitespace";
      case INVALID_NEWLINE -> "invalid newline";
    };
  }


  public String getDetail() {
    return lexeme;
  }

  public enum ErrorType {
    INVALID_CHAR,
    INVALID_CHAR_LITERAL,
    INVALID_COMMENT,
    INVALID_ESCAPE_SEQUENCE,
    INVALID_WHITESPACE,
    INVALID_NEWLINE
  }
}
