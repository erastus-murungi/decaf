package decaf.shared.errors;

import decaf.analysis.Token;
import decaf.analysis.TokenPosition;

public class CfgBuildingError implements Error<CfgBuildingError.ErrorType>{
    private final String detail;
    private final ErrorType errorType;

    public CfgBuildingError(String detail, ErrorType errorType) {
        this.detail = detail;
        this.errorType = errorType;
    }

    @Override
    public String getErrorSummary() {
        return detail;
    }

    @Override
    public String detail() {
        return detail;
    }

    @Override
    public TokenPosition tokenPosition() {
        return TokenPosition.dummyTokenPosition();
    }

    @Override
    public ErrorType errorType() {
        return errorType;
    }

    public enum ErrorType {

    }
}
