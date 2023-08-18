package decaf.shared;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import decaf.shared.errors.Error;

public class CompilationContext {
  public static final String NEW_LINE = "\n";
  private final String sourceCode;
  @NotNull
  private final Logger logger;
  @NotNull
  private Boolean isDebugModeOn;

  public CompilationContext(@NotNull String sourceCode) {
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

  public @NotNull Logger getLogger() {
    return logger;
  }

  public <ErrType extends Enum<ErrType>> String stringifyErrors(@NotNull Collection<? extends Error<ErrType>> errors) {
    return errors.stream()
                 .map(this::stringifyError)
                 .collect(Collectors.joining(NEW_LINE.repeat(2)));
  }

  <ErrType extends Enum<ErrType>> String stringifyError(@NotNull Error<ErrType> error) {
    var output = new ArrayList<String>();

    final String indent = Utils.DEFAULT_INDENT.repeat(3);
    final String subIndent = Utils.DEFAULT_INDENT.repeat(2);
    // print header in sorta Rust style
    output.add(Utils.coloredPrint(
        String.format(
            "%s[%s]",
            error.errorType()
                 .getClass()
                 .getEnclosingClass()
                 .getSimpleName(),
            error.errorType()
                 .name()
        ),
        Utils.ANSIColorConstants.ANSI_RED_BOLD
    ) + ": " + Utils.coloredPrint(
        error.getErrorSummary(),
        Utils.ANSIColorConstants.ANSI_YELLOW
    ));

    output.add(Utils.coloredPrint(
        String.format(
            "%s --> %s",
            subIndent,
            error.tokenPosition()
                 .toString()
        ),
        Utils.ANSIColorConstants.ANSI_CYAN
    ));

    // context before the problematic line
    var sourceCodeLines = sourceCode.split(NEW_LINE);

    var numPrecursorLines = Math.min(
        error.tokenPosition()
             .line(),
        2
    );
    var precursorLines = Arrays.copyOfRange(
        sourceCodeLines,
        error.tokenPosition()
             .line() - numPrecursorLines,
        error.tokenPosition()
             .line()
    );

    var numDigits = (int) Math.log10(sourceCodeLines.length) + 1;
    output.add(
        Utils.identBlockWithNumbering(
            precursorLines,
            indent,
            error.tokenPosition()
                 .line() - numPrecursorLines + 1,
            numDigits
        )
    );

    // context of the problematic line
    var problematicLine = sourceCodeLines[error.tokenPosition()
                                               .line()];
    output.add(Utils.identPointNumberOneLine(
        problematicLine,
        subIndent,
        error.tokenPosition()
             .line() + 1,
        numDigits
    ));

    // underline column of the problematic line
    var underline = Utils.coloredPrint(
        Utils.SPACE.repeat(error.tokenPosition()
                                .column() + numDigits + 3),
        Utils.ANSIColorConstants.ANSI_GREEN_BOLD
    );
    underline += Utils.coloredPrint(
        "^",
        Utils.ANSIColorConstants.ANSI_CYAN
    );

    var detail = Utils.coloredPrint(
        String.format(
            " %s",
            error.detail()
        ),
        Utils.ANSIColorConstants.ANSI_RED_BOLD
    );
    output.add(indent + underline + detail);

    // context after the problematic line
    var numPostCursorLines = Math.min(
        3,
        sourceCodeLines.length - error.tokenPosition()
                                      .line() - 1
    );

    var postCursorLines = Arrays.copyOfRange(
        sourceCodeLines,
        error.tokenPosition()
             .line() + 1,
        error.tokenPosition()
             .line() + 1 + numPostCursorLines
    );
    output.add(
        Utils.identBlockWithNumbering(
            postCursorLines,
            indent,
            error.tokenPosition()
                 .line() + 2
        )
    );

    return String.join(
        NEW_LINE,
        output
    );
  }
}
