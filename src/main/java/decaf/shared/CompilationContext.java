package decaf.shared;

import decaf.analysis.Token;
import decaf.analysis.TokenPosition;
import decaf.shared.errors.ParserError;
import decaf.shared.errors.ScannerError;
import decaf.shared.errors.SemanticError;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import decaf.shared.errors.Error;

public class CompilationContext {
    public static final String NEW_LINE = "\n";
    private final String sourceCode;
    @NotNull
    private final Logger logger;
    private final String filePath;
    @NotNull
    private Boolean isDebugModeOn;

    // errors
    private final List<ScannerError> scanningErrors = new ArrayList<>();
    private List<ParserError> parsingErrors = new ArrayList<>();
    private List<SemanticError> semanticErrors = new ArrayList<>();

    public CompilationContext(@NotNull String sourceCode, boolean debugModeOn, String filePath) {
        this.sourceCode = sourceCode;
        this.logger = Logger.getLogger(CompilationContext.class.getName());
        this.isDebugModeOn = debugModeOn;
        this.filePath = filePath;
    }

    public ScannerError logScannerError(
            TokenPosition tokenPosition,
            ScannerError.ErrorType errorType,
            String detail) {
        var error = new ScannerError(tokenPosition, errorType, detail);
        scanningErrors.add(error);
        return error;
    }

    public void logParsingError(@NotNull Token token,
                                 @NotNull ParserError.ErrorType errorType,
                                 @NotNull String errorMessage) {
        parsingErrors.add(new ParserError(errorType, token, errorMessage));
    }

    public void logSemanticError(@NotNull TokenPosition tokenPosition,
                                 @NotNull SemanticError.ErrorType errorType,
                                 @NotNull String errorMessage) {
        semanticErrors.add(new SemanticError(tokenPosition, errorType, errorMessage));
    }

    public boolean scanningSuccessful() {
        return scanningErrors.isEmpty();
    }

    public boolean semanticCheckingSuccessful() {
        return semanticErrors.isEmpty();
    }

    public boolean encounteredParsingErrors() {
        return !parsingErrors.isEmpty();
    }

    public String getScanningErrorOutput() {
        return stringifyErrors(scanningErrors);
    }

    public String getParsingErrorOutput() {
        return stringifyErrors(parsingErrors);
    }

    public String getSemanticErrorOutput() {
        return stringifyErrors(semanticErrors);
    }

    public void printSemanticErrors() {
        System.err.println(getSemanticErrorOutput());
    }

    public void printParsingErrors() {
        System.err.println(getParsingErrorOutput());
    }

    public boolean parsingSuccessful() {
        return parsingErrors.isEmpty();
    }

    public static CompilationContext fromSourceCode(@NotNull String sourceCode, boolean debugModeOn) {
        return new CompilationContext(sourceCode, debugModeOn, "<no file>");
    }

    public static CompilationContext fromSourceCode(@NotNull String sourceCode) {
        return fromSourceCode(sourceCode, false);
    }

    public static CompilationContext fromFile(@NotNull String filePath,
                                              boolean debugModeOn) throws FileNotFoundException {
        return new CompilationContext(Utils.getStringFromInputStream(new FileInputStream(filePath),
                                                                     Logger.getLogger(CompilationContext.class.getName())
                                                                    ), debugModeOn, filePath);
    }

    public static CompilationContext fromFile(@NotNull String filePath) throws FileNotFoundException {
        return fromFile(filePath, false);
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
        return errors.stream().map(this::stringifyError).collect(Collectors.joining(NEW_LINE.repeat(2)));
    }

    <ErrType extends Enum<ErrType>> String stringifyError(@NotNull Error<ErrType> error) {
        var output = new ArrayList<String>();

        final String indent = Utils.DEFAULT_INDENT.repeat(3);
        final String subIndent = Utils.DEFAULT_INDENT.repeat(2);
        // print header in sorta Rust style
        output.add(Utils.coloredPrint(String.format("%s",
                                                    error.errorType().getClass().getEnclosingClass().getSimpleName()
                                                   ), Utils.ANSIColorConstants.ANSI_RED_BOLD) +
                   ": " +
                   Utils.coloredPrint(error.getErrorSummary(), Utils.ANSIColorConstants.ANSI_PURPLE_BOLD));

        output.add(Utils.coloredPrint(String.format("   %s --> %s:%s",
                                                    subIndent,
                                                    filePath,
                                                    error.tokenPosition().toString()
                                                   ), Utils.ANSIColorConstants.ANSI_CYAN));

        // context before the problematic line
        var sourceCodeLines = sourceCode.split(NEW_LINE);

        var numPrecursorLines = Math.min(error.tokenPosition().line(), 2);
        var precursorLines = Arrays.copyOfRange(sourceCodeLines,
                                                error.tokenPosition().line() - numPrecursorLines,
                                                error.tokenPosition().line()
                                               );

        var numDigits = (int) Math.log10(sourceCodeLines.length) + 1;
        output.add(Utils.identBlockWithNumbering(precursorLines,
                                                 indent,
                                                 error.tokenPosition().line() - numPrecursorLines + 1,
                                                 numDigits
                                                ));

        // context of the problematic line
        var problematicLine = sourceCodeLines[error.tokenPosition().line()];
        output.add(Utils.identPointNumberOneLine(problematicLine,
                                                 subIndent,
                                                 error.tokenPosition().line() + 1,
                                                 numDigits
                                                ));

        // underline column of the problematic line
        var underline = Utils.coloredPrint(Utils.SPACE.repeat(error.tokenPosition().column() + numDigits + 3),
                                           Utils.ANSIColorConstants.ANSI_GREEN_BOLD
                                          );
        underline += Utils.coloredPrint("^", Utils.ANSIColorConstants.ANSI_CYAN);

        var detail = Utils.coloredPrint(String.format(" %s", error.detail()), Utils.ANSIColorConstants.ANSI_RED_BOLD);
        output.add(indent + underline + detail);

        // context after the problematic line
        var numPostCursorLines = Math.min(3, sourceCodeLines.length - error.tokenPosition().line() - 1);

        var postCursorLines = Arrays.copyOfRange(sourceCodeLines,
                                                 error.tokenPosition().line() + 1,
                                                 error.tokenPosition().line() + 1 + numPostCursorLines
                                                );
        output.add(Utils.identBlockWithNumbering(postCursorLines, indent, error.tokenPosition().line() + 2));

        return String.join(NEW_LINE, output);
    }
}
