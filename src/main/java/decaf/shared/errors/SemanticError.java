package decaf.shared.errors;

import org.jetbrains.annotations.NotNull;

import decaf.analysis.TokenPosition;

public record SemanticError(TokenPosition tokenPosition,
                            @NotNull SemanticError.ErrorType errorType,
                            String detail) implements Error<SemanticError.ErrorType> {

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
      case METHOD_CALL_CONFLICTS_WITH_LOCALLY_DEFINED_IDENTIFIER ->
          "method call conflicts with locally defined identifier";
      case MISMATCHING_NUMBER_OR_ARGUMENTS -> "mismatching number of arguments";
      case INCORRECT_ARG_TYPE -> "incorrect argument type";
      case SHADOWING_FORMAL_ARGUMENT -> "shadowing parameter";
      case INVALID_MAIN_METHOD -> "invalid main method";
      case MISSING_MAIN_METHOD -> "missing main method";
      case METHOD_ALREADY_DEFINED -> "method already defined";
      case BREAK_OUT_OF_CONTEXT -> "break statement not enclosed";
      case CONTINUE_OUT_OF_CONTEXT -> "continue statement not enclosed";
      case INVALID_ARRAY_SIZE -> "invalid array size";
      case INVALID_ARGUMENT_TYPE -> "invalid argument type";
      case MISMATCHING_RETURN_TYPE -> "mismatching return type";
      case MISSING_RETURN_STATEMENT -> "missing return statement";
      case SHADOWING_IMPORT -> "shadowing import";
        case TYPE_MISMATCH -> "type mismatch";
    };
  }

  @Override
  public ErrorType errorType() {
    return errorType;
  }

  public enum ErrorType {
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
    SHADOWING_FORMAL_ARGUMENT,
    SHADOWING_IMPORT,
    INVALID_MAIN_METHOD,
    MISSING_MAIN_METHOD,
    METHOD_ALREADY_DEFINED,
    BREAK_OUT_OF_CONTEXT,
    CONTINUE_OUT_OF_CONTEXT,
    INVALID_ARRAY_SIZE,
    INVALID_ARGUMENT_TYPE,
    MISMATCHING_RETURN_TYPE,
    MISSING_RETURN_STATEMENT,

    TYPE_MISMATCH,
  }
}
