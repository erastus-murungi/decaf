package decaf.errors;

import decaf.grammar.TokenPosition;

public interface Error<ErrorType extends Enum<ErrorType>> {
  String getErrorSummary();
  String getDetail();
  TokenPosition tokenPosition();
  ErrorType errorType();
}
