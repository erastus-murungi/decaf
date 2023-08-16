package decaf.shared.errors;

import decaf.analysis.TokenPosition;

public interface Error<ErrorType extends Enum<ErrorType>> {
  String getErrorSummary();
  String getDetail();
  TokenPosition tokenPosition();
  ErrorType errorType();
}
