package decaf.grammar;

public record ParserError(decaf.grammar.ParserError.ErrorType errorType, Token token, String detail) {

  public String getErrorSummary() {
    return switch (errorType) {
      case UNEXPECTED_TOKEN -> "Unexpected token";
      case DISALLOWED_RETURN_TYPE -> "Method cannot return a value";
      case MISSING_ARRAY_SIZE -> "Missing array size";
      case MISSING_RETURN_TYPE -> "Missing return type";
      case MISSING_FIELD_TYPE -> "Missing field type";
      case EXTRA_TOKENS_AFTER_PROGRAM_END -> "Extra tokens after program end";
      case MISSING_METHOD_ARGUMENT_TYPE -> "Missing method argument type";
      case ILLEGAL_ARGUMENT_TYPE -> "Illegal argument type";
      case MISSING_CONDITIONAL_EXPRESSION -> "Missing conditional expression";
      case INVALID_FIELD_TYPE -> "Invalid field type";
      case MISSING_NAME -> "Missing name";
      case MISSING_IMPORT_NAME -> "Missing import name";
      case MISSING_SEMICOLON -> "Missing semicolon";
      case DID_NOT_FINISH_PARSING -> "Did not finish parsing";
      case MISSING_RIGHT_PARENTHESIS -> "Missing right parenthesis";
      case MISSING_RIGHT_SQUARE_BRACKET -> "Missing right square bracket";
      case INVALID_TYPE -> "Invalid type";
    };
  }


  public enum ErrorType {
    UNEXPECTED_TOKEN,
    DISALLOWED_RETURN_TYPE,
    MISSING_ARRAY_SIZE,
    MISSING_RETURN_TYPE,
    MISSING_FIELD_TYPE,
    EXTRA_TOKENS_AFTER_PROGRAM_END,
    MISSING_METHOD_ARGUMENT_TYPE,
    ILLEGAL_ARGUMENT_TYPE,
    MISSING_CONDITIONAL_EXPRESSION,
    INVALID_FIELD_TYPE,
    MISSING_NAME,
    MISSING_IMPORT_NAME,
    MISSING_SEMICOLON,
    DID_NOT_FINISH_PARSING,
    MISSING_RIGHT_PARENTHESIS,
    MISSING_RIGHT_SQUARE_BRACKET,
    INVALID_TYPE,
  }
}
