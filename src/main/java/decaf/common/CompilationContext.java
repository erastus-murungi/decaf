package decaf.common;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import decaf.exceptions.DecafParserException;
import decaf.exceptions.DecafSemanticException;
import decaf.grammar.ScannerError;
import decaf.grammar.Token;
import decaf.grammar.TokenPosition;

public class CompilationContext {
  public static final String NEW_LINE = "\n";
  public static final String TERNARY_COLON = ":";
  public static final String TILDE = "~";
  public final int MAX_NUM_CHARS = 30;
  private String asmOutputFilename;
  private String sourceFilename;
  private final String sourceCode;
  private static boolean debugModeOn;

  CompilationContext(@NotNull String sourceCode) {
    this.sourceCode = sourceCode;
  }

  public String getAsmOutputFilename() {
    return asmOutputFilename;
  }

  public void setAsmOutputFilename(String asmOutputFilename) {
    this.asmOutputFilename = asmOutputFilename;
  }

  public String getSourceFilename() {
    return sourceFilename;
  }

  public void setSourceFilename(String sourceFilename) {
    this.sourceFilename = sourceFilename;
  }

  public static boolean isDebugModeOn() {
    return debugModeOn;
  }

  public static void setDebugModeOn(boolean debugModeOn) {
    CompilationContext.debugModeOn = debugModeOn;
  }


  public String getContextualErrorMessage(
      TokenPosition tokenPosition,
      String errMessage
  ) {
    final String lineToPrint = sourceCode.split(NEW_LINE)[tokenPosition.line()];
    final int before = Math.min(
        tokenPosition.column(),
        MAX_NUM_CHARS
    );
    final int after = Math.min(
        lineToPrint.length() - tokenPosition.column(),
        MAX_NUM_CHARS
    );

    final String inputSub = lineToPrint.substring(
        tokenPosition.column() - before,
        tokenPosition.column() + after
    );
    final String spaces = Utils.SPACE.repeat(String.valueOf(tokenPosition.line())
                                                   .length() + String.valueOf(tokenPosition.column())
                                                                     .length() + 3);

    return NEW_LINE + errMessage + NEW_LINE + spaces + inputSub + NEW_LINE + tokenPosition.line() + TERNARY_COLON +
        tokenPosition.column() + TERNARY_COLON + Utils.SPACE + Utils.coloredPrint(
        TILDE.repeat(before),
        Utils.ANSIColorConstants.ANSI_GREEN
    ) + Utils.coloredPrint(
        "^",
        Utils.ANSIColorConstants.ANSI_CYAN
    ) + Utils.coloredPrint(
        TILDE.repeat(after),
        Utils.ANSIColorConstants.ANSI_GREEN
    );
  }

  public DecafParserException getContextualDecafParserException(
      Token token,
      String errMessage
  ) {
    return new DecafParserException(
        token,
        getContextualErrorMessage(
            token.tokenPosition,
            errMessage
        )
    );
  }

  public DecafSemanticException processDecafSemanticException(DecafSemanticException decafSemanticException) {
    TokenPosition tokenPosition = new TokenPosition(
        decafSemanticException.line,
        decafSemanticException.column,
        -1
    );
    DecafSemanticException decafSemanticException1 = new DecafSemanticException(
        tokenPosition,
        getContextualErrorMessage(
            tokenPosition,
            decafSemanticException.getMessage()
        )
    );
    decafSemanticException1.setStackTrace(decafSemanticException.getStackTrace());
    //            System.err.println(decafSemanticException.getMessage());
    return decafSemanticException1;
  }

  public String processScannerErrorOutput(@NotNull ScannerError scannerError) {
    var output = new ArrayList<String>();

    final String indent = Utils.DEFAULT_INDENT.repeat(3);
    final String subIndent = Utils.DEFAULT_INDENT.repeat(2);
    // print header in sorta Rust style
    output.add(Utils.coloredPrint(
        String.format(
            "error[%s]",
            scannerError.getErrorType()
                        .toString()
        ),
        Utils.ANSIColorConstants.ANSI_RED_BOLD
    ) + ": " + Utils.coloredPrint(
        scannerError.getErrorSummary(),
        Utils.ANSIColorConstants.ANSI_YELLOW
    ));

    // context before the problematic line
    var sourceCodeLines = sourceCode.split(NEW_LINE);

    var numPrecursorLines = Math.max(
        0,
        scannerError.tokenPosition.line()
    );
    var precursorLines = Arrays.copyOfRange(
        sourceCodeLines,
        0,
        numPrecursorLines
    );

    var numDigits = (int) Math.log10(sourceCodeLines.length) + 1;
    output.add(
        Utils.identBlockWithNumbering(
            precursorLines,
            indent,
            1,
            numDigits
        )
    );

    // context of the problematic line
    var problematicLine = sourceCodeLines[scannerError.tokenPosition.line()];
    output.add(Utils.identPointNumberOneLine(
        problematicLine,
        subIndent,
        scannerError.tokenPosition.line() + 1,
        numDigits
    ));

    // underline column of the problematic line
    var underline = Utils.coloredPrint(
        Utils.SPACE.repeat(scannerError.tokenPosition.column() + numDigits + 3),
        Utils.ANSIColorConstants.ANSI_GREEN_BOLD
    );
    underline += Utils.coloredPrint(
        "^",
        Utils.ANSIColorConstants.ANSI_CYAN
    );

    var detail = Utils.coloredPrint(
        String.format(
            " %s",
            scannerError.getDetail()
        ),
        Utils.ANSIColorConstants.ANSI_RED_BOLD
    );
    output.add(indent + underline + detail);

    // context after the problematic line
    var numPostCursorLines = Math.min(
        3,
        sourceCodeLines.length - scannerError.tokenPosition.line() - 1
    );

    var postCursorLines = Arrays.copyOfRange(
        sourceCodeLines,
        scannerError.tokenPosition.line() + 1,
        scannerError.tokenPosition.line() + 1 + numPostCursorLines
    );
    output.add(
        Utils.identBlockWithNumbering(
            postCursorLines,
            indent,
            scannerError.tokenPosition.line() + 2
        )
    );

    return String.join(
        NEW_LINE,
        output
    );
  }

  public String processScannerErrorOutput(@NotNull Collection<ScannerError> scannerErrorsCollection) {
    return scannerErrorsCollection.stream()
                                  .map(this::processScannerErrorOutput)
                                  .collect(Collectors.joining(NEW_LINE.repeat(2)));
  }
}
