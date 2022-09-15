package decaf.common;

import decaf.grammar.Token;
import decaf.grammar.TokenPosition;
import decaf.exceptions.DecafParserException;
import decaf.exceptions.DecafScannerException;
import decaf.exceptions.DecafSemanticException;

public class DecafExceptionProcessor {
    public static final String NEW_LINE = "\n";
    public static final String TERNARY_COLON = ":";
    public static final String TILDE = "~";
    public final int MAX_NUM_CHARS = 30;
    public String syntaxHighlightedSourceCode;
    String sourceCode;

    public DecafExceptionProcessor(String sourceCode) {
        this.sourceCode = sourceCode;
    }

    public String getContextualErrorMessage(TokenPosition tokenPosition, String errMessage) {
        final String lineToPrint = sourceCode.split(NEW_LINE)[tokenPosition.line()];
        final int before = Math.min(tokenPosition.column(), MAX_NUM_CHARS);
        final int after = Math.min(lineToPrint.length() - tokenPosition.column(), MAX_NUM_CHARS);

        final String inputSub = lineToPrint.substring(tokenPosition.column() - before, tokenPosition.column() + after);
        final String spaces = Utils.SPACE.repeat(String.valueOf(tokenPosition.line())
                .length() + String.valueOf(tokenPosition.column())
                .length() + 3);

        return NEW_LINE + errMessage + NEW_LINE + spaces + inputSub + NEW_LINE + tokenPosition.line() + TERNARY_COLON + tokenPosition.column() + TERNARY_COLON + Utils.SPACE + Utils.coloredPrint(TILDE.repeat(before), Utils.ANSIColorConstants.ANSI_GREEN) + Utils.coloredPrint("^", Utils.ANSIColorConstants.ANSI_CYAN) + Utils.coloredPrint(TILDE.repeat(after), Utils.ANSIColorConstants.ANSI_GREEN);
    }

    public DecafScannerException getContextualDecafScannerException(Token prevToken, char currentChar, TokenPosition tokenPosition, String errMessage) {
        if (prevToken != null) {
            switch (prevToken.tokenType()) {
                case STRING_LITERAL: {
                    errMessage = "illegal string literal: " + prevToken.lexeme() + currentChar;
                    break;
                }
                case ID: {
                    errMessage = "illegal id: " + prevToken.lexeme() + currentChar;
                    break;
                }
                default: {
                    break;
                }
            }
        }
        return new DecafScannerException(tokenPosition, getContextualErrorMessage(tokenPosition, errMessage));
    }

    public DecafParserException getContextualDecafParserException(Token token, String errMessage) {
        return new DecafParserException(token, getContextualErrorMessage(token.tokenPosition, errMessage));
    }

    public DecafSemanticException processDecafSemanticException(DecafSemanticException decafSemanticException) {
        TokenPosition tokenPosition = new TokenPosition(decafSemanticException.line, decafSemanticException.column, -1);
        DecafSemanticException decafSemanticException1 = new DecafSemanticException(tokenPosition, getContextualErrorMessage(tokenPosition, decafSemanticException.getMessage()));
        decafSemanticException1.setStackTrace(decafSemanticException.getStackTrace());
        //            System.err.println(decafSemanticException.getMessage());
        return decafSemanticException1;
    }
}
