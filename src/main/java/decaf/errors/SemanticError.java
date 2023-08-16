package decaf.errors;

import org.jetbrains.annotations.NotNull;

import decaf.grammar.TokenPosition;

public class SemanticError implements Error<SemanticError.SemanticErrorType>{
  private final TokenPosition tokenPosition;
  @NotNull private final SemanticErrorType errorType;
  private final String detail;

  public SemanticError(
      TokenPosition tokenPosition,
      @NotNull SemanticErrorType errorType,
      String detail
  ) {
    this.tokenPosition = tokenPosition;
    this.errorType = errorType;
    this.detail = detail;
  }

  @Override
  public String getErrorSummary() {
    return switch (errorType) {
      case IDENTIFIER_ALREADY_DECLARED -> "identifier already declared";
      case IDENTIFIER_NOT_IN_SCOPE -> "identifier not in scope";
      case SHOULD_RETURN_VOID -> "should return void";
      case METHOD_DEFINITION_NOT_FOUND -> "method definition not found";
      case INT_LITERAL_TOO_BIG -> "int literal too big";
      case UNSUPPORTED_TYPE -> "unsupported type";
      case INVALID_ARRAY_INDEX -> "invalid array index";
      case METHOD_CALL_CONFLICTS_WITH_LOCALLY_DEFINED_IDENTIFIER -> "method call conflicts with locally defined identifier";
      case MISMATCHING_NUMBER_OR_ARGUMENTS -> "mismatching number of arguments";
      case INCORRECT_ARG_TYPE -> "incorrect argument type";
      case SHADOWING_PARAMETER -> "shadowing parameter";
      case INVALID_MAIN_METHOD -> "invalid main method";
      case MISSING_MAIN_METHOD -> "missing main method";
      case METHOD_ALREADY_DEFINED -> "method already defined";
      case BREAK_STATEMENT_NOT_ENCLOSED -> "break statement not enclosed";
      case CONTINUE_STATEMENT_NOT_ENCLOSED -> "continue statement not enclosed";
      case INVALID_ARRAY_SIZE -> "invalid array size";
      case INVALID_ARGUMENT_TYPE -> "invalid argument type";
      case MISMATCHING_RETURN_TYPE -> "mismatching return type";
      case MISSING_RETURN_STATEMENT -> "missing return statement";
    };
  }

  @Override
  public String getDetail() {
    return detail;
  }

  @Override
  public TokenPosition tokenPosition() {
    return tokenPosition;
  }

  @Override
  public SemanticErrorType errorType() {
    return errorType;
  }

  public enum SemanticErrorType {
    IDENTIFIER_ALREADY_DECLARED,
    IDENTIFIER_NOT_IN_SCOPE,
    SHOULD_RETURN_VOID,
    METHOD_DEFINITION_NOT_FOUND,
    INT_LITERAL_TOO_BIG,
    UNSUPPORTED_TYPE,
    INVALID_ARRAY_INDEX,
    METHOD_CALL_CONFLICTS_WITH_LOCALLY_DEFINED_IDENTIFIER,
    MISMATCHING_NUMBER_OR_ARGUMENTS,
    INCORRECT_ARG_TYPE,
    SHADOWING_PARAMETER,
    INVALID_MAIN_METHOD,
    MISSING_MAIN_METHOD,
    METHOD_ALREADY_DEFINED,
    BREAK_STATEMENT_NOT_ENCLOSED,
    CONTINUE_STATEMENT_NOT_ENCLOSED,
    INVALID_ARRAY_SIZE,
    INVALID_ARGUMENT_TYPE,
    MISMATCHING_RETURN_TYPE,
    MISSING_RETURN_STATEMENT,
  }
}
