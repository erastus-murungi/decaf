package decaf.common;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import decaf.exceptions.DecafSemanticException;
import decaf.grammar.ParserError;
import decaf.grammar.ScannerError;
import decaf.grammar.TokenPosition;

public class CompilationContext {
  public static final String NEW_LINE = "\n";
  public static final String TERNARY_COLON = ":";
  public static final String TILDE = "~";
  public final int MAX_NUM_CHARS = 30;
  private String asmOutputFilename;
  private String sourceFilename;
  private final String sourceCode;
  @NotNull private final Logger logger;
  @NotNull private Boolean isDebugModeOn;

  CompilationContext(@NotNull String sourceCode) {
    this.sourceCode = sourceCode;
    this.logger = Logger.getLogger(CompilationContext.class.getName());
    this.isDebugModeOn = false;
  }

  CompilationContext(@NotNull String sourceCode, boolean debugModeOn) {
    this.sourceCode = sourceCode;
    this.logger = Logger.getLogger(CompilationContext.class.getName());
    this.isDebugModeOn = debugModeOn;
  }

  public boolean debugModeOn() {
    return isDebugModeOn;
  }

  public void setDebugMode(boolean debugMode) {
    this.isDebugModeOn = debugMode;
  }

  public Logger getLogger() {
    return logger;
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

  public DecafSemanticException processDecafSemanticException(DecafSemanticException decafSemanticException) {
    TokenPosition tokenPosition = new TokenPosition(
        decafSemanticException.line,
        decafSemanticException.column,
        -1
    );
    DecafSemanticException decafSemanticException1 = new DecafSemanticException(
        tokenPosition,
        decafSemanticException.getMessage()
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
            "scanning_error[%s]",
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

  public String processParserErrorOutput(@NotNull ParserError parserError) {
    var output = new ArrayList<String>();

    final String indent = Utils.DEFAULT_INDENT.repeat(3);
    final String subIndent = Utils.DEFAULT_INDENT.repeat(2);
    // print header in sorta Rust style
    output.add(Utils.coloredPrint(
        String.format(
            "parsing_error[%s]",
            parserError.errorType()
                        .toString()
        ),
        Utils.ANSIColorConstants.ANSI_RED_BOLD
    ) + ": " + Utils.coloredPrint(
        parserError.getErrorSummary(),
        Utils.ANSIColorConstants.ANSI_YELLOW
    ));

    // context before the problematic line
    var sourceCodeLines = sourceCode.split(NEW_LINE);

    var numPrecursorLines = Math.max(
        0,
        parserError.token().tokenPosition.line()
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
    var problematicLine = sourceCodeLines[parserError.token().tokenPosition.line()];
    output.add(Utils.identPointNumberOneLine(
        problematicLine,
        subIndent,
        parserError.token().tokenPosition.line() + 1,
        numDigits
    ));

    // underline column of the problematic line
    var underline = Utils.coloredPrint(
        Utils.SPACE.repeat(parserError.token().tokenPosition.column() + numDigits + 3),
        Utils.ANSIColorConstants.ANSI_GREEN_BOLD
    );
    underline += Utils.coloredPrint(
        "^",
        Utils.ANSIColorConstants.ANSI_CYAN
    );

    var detail = Utils.coloredPrint(
        String.format(
            " %s",
            parserError.detail()
        ),
        Utils.ANSIColorConstants.ANSI_RED_BOLD
    );
    output.add(indent + underline + detail);

    // context after the problematic line
    var numPostCursorLines = Math.min(
        3,
        sourceCodeLines.length - parserError.token().tokenPosition.line() - 1
    );

    var postCursorLines = Arrays.copyOfRange(
        sourceCodeLines,
        parserError.token().tokenPosition.line() + 1,
        parserError.token().tokenPosition.line() + 1 + numPostCursorLines
    );
    output.add(
        Utils.identBlockWithNumbering(
            postCursorLines,
            indent,
            parserError.token().tokenPosition.line() + 2
        )
    );

    return String.join(
        NEW_LINE,
        output
    );
  }

  public String processParserErrorOutput(@NotNull Collection<ParserError> parserErrorsCollection) {
    return parserErrorsCollection.stream()
                                  .map(this::processParserErrorOutput)
                                  .collect(Collectors.joining(NEW_LINE.repeat(2)));
  }
}
