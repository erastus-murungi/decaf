package decaf.shared;

import static java.util.logging.Level.SEVERE;

import com.google.common.base.Stopwatch;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import decaf.analysis.lexical.Scanner;
import decaf.analysis.semantic.SemanticChecker;
import decaf.analysis.syntax.Parser;

public class TestRunner {
  private static final Logger logger =
      Logger.getLogger(TestRunner.class.getCanonicalName());

  private static String readFile(File file) throws IOException {
    var reader = new BufferedReader(new FileReader(file));
    return reader.lines()
                 .collect(Collectors.joining(System.lineSeparator()));
  }

  private static @NotNull List<Pair<File, File>>
  getTestPairs(
      @NotNull String filepathPrefix, boolean expectedToPass,
      boolean outputExists
  ) {
    var filepath =
        filepathPrefix + "/input/" + (expectedToPass ? "/legal": "/illegal");
    try (var files = Files.list(Paths.get(filepath))) {
      if (outputExists) {
        return files.map(Path::toFile)
                    .filter(File::isFile)
                    .sorted(Comparator.comparing(File::getName))
                    .map(file
                             -> new Pair<>(
                        file,
                        Paths
                            .get(filepathPrefix + "/output/" +
                                     file.getName()
                                         .substring(
                                             0,
                                             file.getName()
                                                 .length() - 4
                                         ) +
                                     ".out")
                            .toFile()
                    ))
                    .toList();
      } else {
        return files.map(Path::toFile)
                    .filter(File::isFile)
                    .sorted(Comparator.comparing(File::getName))
                    .map(file -> new Pair<>(
                        file,
                        file
                    ))
                    .toList();
      }
    } catch (IOException e) {
      logger.log(
          SEVERE,
          "Failed to compile tests due to an IO error",
          e
      );
      return Collections.emptyList();
    }
  }

  private static void printFailError(
      int pairIndex, int numTests,
      String filePath, long runTimeMs,
      String reason
  ) {
    System.out.format(
        "%s %s ........... (%s ms)\n%s\n",
        Utils.coloredPrint(
            String.format(
                "\t✗ [%s/%s] ",
                pairIndex,
                numTests
            ),
            Utils.ANSIColorConstants.ANSI_BRIGHT_RED
        ),
        Utils.coloredPrint(
            filePath,
            Utils.ANSIColorConstants.LIGHT_GREY
        ),
        runTimeMs,
        Utils.identBlockWithNumbering(
            Utils.coloredPrint(
                reason,
                Utils.ANSIColorConstants.ANSI_WHITE
            ),
            3
        )
    );
  }

  private static void printSuccessError(
      int pairIndex, int numTests,
      String filePath, long runTimeMs
  ) {
    System.out.format(
        "%s %s ........... (%s ms)\n",
        Utils.coloredPrint(
            String.format(
                "\t✓ [%s/%s] ",
                pairIndex,
                numTests
            ),
            Utils.ANSIColorConstants.ANSI_GREEN_BOLD
        ),
        Utils.coloredPrint(
            filePath,
            Utils.ANSIColorConstants.LIGHT_GREY
        ),
        runTimeMs
    );
  }

  private static void printFailedSuccessfully(
      int pairIndex, int numTests,
      String filePath, long runTimeMs,
      String actualFailReason
  ) {
    System.out.format(
        """
            %s %s ........... (%s ms)
            %s
            """,
        Utils.coloredPrint(
            String.format(
                "\t✓ [%s/%s] ",
                pairIndex,
                numTests
            ),
            Utils.ANSIColorConstants.ANSI_GREEN_BOLD
        ),
        Utils.coloredPrint(
            filePath,
            Utils.ANSIColorConstants.LIGHT_GREY
        ),
        runTimeMs,
        Utils.coloredPrint(
            actualFailReason,
            Utils.ANSIColorConstants.ANSI_WHITE
        )
    );
  }

  private static String getTestingEpilogue(
      Type type, String filePath, long totalTimeElapsed,
      List<Quadruple<String, Integer, Integer, Long>> testSuiteResults
  ) {
    var strings = new ArrayList<String>();
    strings.add(String.format(
        "Test Suites Summary, %s",
        type
    ));
    var allTestsNumPassed = 0;
    var allTestsNumTests = 0;
    for (var testSuiteResult : testSuiteResults) {
      var testName = testSuiteResult.first();
      var numPassed = testSuiteResult.second();
      var numTests = testSuiteResult.third();
      var timeElapsed = testSuiteResult.fourth();
      allTestsNumPassed += numPassed;
      allTestsNumTests += numTests;
      if (numPassed.equals(numTests)) {
        strings.add(String.format(
            "\t\t%s: %s",
            testName,
            Utils.coloredPrint(
                String.format("✓ Passed all %s tests in %s ms",
                              numTests,
                              timeElapsed
                ),
                Utils.ANSIColorConstants.ANSI_GREEN_BOLD
            )
        ));
      } else {
        strings.add(Utils.coloredPrint(
            String.format("\t\t%s: ✗ Passed %s out of %s tests in %s ms",
                          testName,
                          numPassed,
                          numTests,
                          timeElapsed
            ),
            Utils.ANSIColorConstants.ANSI_RED
        ));
      }
    }
    strings.add(String.format(
        "%s %s",
        (allTestsNumPassed == allTestsNumTests)
            ? Utils.coloredPrint(
            "PASS",
            Utils.ANSIColorConstants.ANSI_BG_GREEN
        )
            : Utils.coloredPrint(
            "FAIL",
            Utils.ANSIColorConstants.ANSI_BG_RED
        ),
        Utils.coloredPrint(
            String.format(
                "%s",
                filePath
            ),
            Utils.ANSIColorConstants.ANSI_WHITE
        )
    ));
    if (allTestsNumPassed == allTestsNumTests) {
      strings.add(
          Utils.coloredPrint(
              String.format("✓ Passed all %s tests in %s ms",
                            allTestsNumTests,
                            totalTimeElapsed
              ),
              Utils.ANSIColorConstants.ANSI_GREEN_BOLD
          ));
    } else {
      strings.add(Utils.coloredPrint(
          String.format("✗ Passed %s out of %s tests in %s ms",
                        allTestsNumPassed,
                        allTestsNumTests,
                        totalTimeElapsed
          ),
          Utils.ANSIColorConstants.ANSI_RED
      ));
    }
    return String.join(
        "\n",
        strings
    );
  }

  private static String formatTokensToOutputFormat(@NotNull Scanner scanner) {
    var strings = new ArrayList<String>();
    for (var token : scanner) {
      var text = switch (token.type) {
        case ID -> "IDENTIFIER" + " " + token.lexeme;
        case STRING_LITERAL -> "STRINGLITERAL" + " " + token.lexeme;
        case CHAR_LITERAL -> "CHARLITERAL" + " " + token.lexeme;
        case HEX_LITERAL, DECIMAL_LITERAL -> "INTLITERAL" + " " + token.lexeme;
        case RESERVED_FALSE, RESERVED_TRUE -> "BOOLEANLITERAL" + " " + token.lexeme;
        case EOF -> "";
        default -> token.lexeme;
      };
      if (text.isBlank())
        continue;
      strings.add(token.tokenPosition
                      .line() + 1 + " " + text);
    }
    return String.join(
        "\n",
        strings
    );
  }

  public static void testOneFile(
      @NotNull Type type,
      @NotNull String filename,
      boolean hasOutput,
      boolean expectedToPass,
      boolean verbose
  ) {
    var filepath = getTestsFilePathForType(type);
    var input = Paths.get(filepath + "/input/" + (expectedToPass ? "legal/": "illegal/") + filename + ".dcf")
                     .toFile();
    var output =
        hasOutput ? Paths.get(filepath + "/output/" + filename + ".out")
                         .toFile()
            : input;
    var testPairs = List.of(new Pair<>(
        input,
        output
    ));
    testSuite(testPairs,
              expectedToPass,
              getTestFunctionForType(type),
              verbose
    );
  }

  public static boolean testAll(boolean verbose) {
    for (var type : Type.values()) {
      if (!testOneComponent(
          type,
          type == Type.SCANNER,
          verbose
      )) {
        return false;
      }
    }
    return true;
  }

  public static boolean testAll() {
    return testAll(false);
  }

  public static void testScanner(
      @NotNull String filename, boolean expectedToPass, boolean verbose
  ) {
    testOneFile(
        Type.SCANNER,
        filename,
        true,
        expectedToPass,
        verbose
    );
  }

  public static void testParser(
      @NotNull String filename,
      boolean expectedToPass, boolean verbose
  ) {
    testOneFile(
        Type.PARSER,
        filename,
        false,
        expectedToPass,
        verbose
    );
  }

  public static void testSemantics(
      @NotNull String filename, boolean expectedToPass, boolean verbose
  ) {
    testOneFile(
        Type.SEMANTICS,
        filename,
        false,
        expectedToPass,
        verbose
    );
  }

  public static boolean testScanner(boolean verbose) {
    return testOneComponent(
        Type.SCANNER,
        true,
        verbose
    );
  }

  public static boolean testScanner() {
    return testScanner(true);
  }

  public static boolean testSemantics(boolean verbose) {
    return testOneComponent(
        Type.SEMANTICS,
        false,
        verbose
    );
  }

  public static boolean testSemantics() {
    return testSemantics(true);
  }

  public static boolean testParser(boolean verbose) {
    return testOneComponent(
        Type.PARSER,
        false,
        verbose
    );
  }

  public static boolean testParser() {
    return testParser(true);
  }

  public static Pair<Integer, Long> testSuite(
      List<Pair<File, File>> testPairs, boolean expectedToPass,
      BiFunction<String, String, Pair<Boolean, String>> fn,
      boolean verbose
  ) {
    final int numTests = testPairs.size();
    int numPassed = 0;
    Stopwatch testSuiteTestTime = Stopwatch.createStarted();
    for (var pairIndex = 0; pairIndex < testPairs.size(); pairIndex++) {
      Stopwatch stopwatch = Stopwatch.createStarted();
      var pair = testPairs.get(pairIndex);
      var sourceFile = pair.first;
      try {
        var sourceCode = readFile(pair.first);
        var expectedOutput = readFile(pair.second);
        numPassed +=
            testOnePair(fn,
                        sourceCode,
                        expectedOutput,
                        pairIndex,
                        numTests,
                        sourceFile.getAbsolutePath(),
                        expectedToPass,
                        verbose
            )
                ? 1
                : 0;
      } catch (IOException e) {
        if (verbose) {
          printFailError(
              pairIndex,
              numTests,
              sourceFile.getAbsolutePath(),
              stopwatch.elapsed(TimeUnit.MILLISECONDS),
              e.getMessage()
          );
        }
      }
    }
    return new Pair<>(
        numPassed,
        testSuiteTestTime.elapsed(TimeUnit.MILLISECONDS)
    );
  }

  public static Pair<Boolean, String> scannerTestFunction(
      String input,
      String output
  ) {
    var scanner = new Scanner(
        input,
        new CompilationContext(input)
    );
    var actualOutput = formatTokensToOutputFormat(scanner);
    if (scanner.finished() && actualOutput.equals(output)) {
      return new Pair<>(
          true,
          actualOutput
      );
    } else {
      return new Pair<>(
          false,
          scanner.getPrettyErrorOutput()
      );
    }
  }

  public static Pair<Boolean, String> parserTestFunction(
      String input,
      String output
  ) {
    var scanner = new Scanner(
        input,
        new CompilationContext(input)
    );
    var parser = new Parser(
        scanner,
        new CompilationContext(input)
    );
    if (parser.getErrors()
              .isEmpty()) {
      return new Pair<>(
          true,
          ""
      );
    } else {
      return new Pair<>(
          false,
          parser.getPrettyErrorOutput()
      );
    }
  }

  public static Pair<Boolean, String> semanticsTestFunction(
      String input, String output
  ) {
    var context = new CompilationContext(input);
    var scanner = new Scanner(
        input,
        context
    );
    if (!scanner.finished()) {
      return new Pair<>(
          false,
          scanner.getPrettyErrorOutput()
      );
    }
    var parser = new Parser(
        scanner,
        context
    );
    if (!parser.getErrors()
               .isEmpty()) {
      return new Pair<>(
          false,
          parser.getPrettyErrorOutput()
      );
    }
    var semanticChecker = new SemanticChecker(
        parser.getRoot(),
        context
    );
    if (semanticChecker.hasErrors()) {
      return new Pair<>(
          false,
          semanticChecker.getPrettyErrorOutput()
      );
    }
    return new Pair<>(
        true,
        ""
    );
  }

  private static boolean testOneComponent(
      @NotNull Type type, boolean outputExists, boolean verbose
  ) {
    var filepath = getTestsFilePathForType(type);
    var testFunctionForType = getTestFunctionForType(type);
    var startTime = Stopwatch.createStarted();
    var expectedToPassTestPairs =
        getTestPairs(
            filepath,
            true,
            outputExists
        );
    var expectedToPassResults = testSuite(expectedToPassTestPairs,
                                          true,
                                          testFunctionForType,
                                          verbose
    );

    var expectedToFailTestPairs =
        getTestPairs(
            filepath,
            false,
            outputExists
        );
    var expectedToFailResults = testSuite(expectedToFailTestPairs,
                                          false,
                                          testFunctionForType,
                                          verbose
    );

    System.out.println(getTestingEpilogue(
        type,
        filepath,
        startTime.elapsed(TimeUnit.MILLISECONDS),
        List.of(
            new Quadruple<>("legal",
                            expectedToPassResults.first(),
                            expectedToPassTestPairs.size(),
                            expectedToPassResults.second()
            ),
            new Quadruple<>("illegal",
                            expectedToFailResults.first(),
                            expectedToFailTestPairs.size(),
                            expectedToFailResults.second()
            )
        )
    ));
    return expectedToPassResults.first()
                                .equals(
                                    expectedToPassTestPairs.size()) &&
        expectedToFailResults.first()
                             .equals(
                                 expectedToFailTestPairs.size());
  }

  private static <InputType, OutputType> boolean testOnePair(
      @NotNull BiFunction<InputType, OutputType, Pair<Boolean, String>>
          fn,
      @NotNull InputType input, @Nullable OutputType expectedOutput,
      int pairIndex, int numTests, @Nullable String sourceFilePath,
      boolean expectedToPass, boolean shouldPrint
  ) {
    var stopwatch = Stopwatch.createStarted();
    var ret = fn.apply(
        input,
        expectedOutput
    );
    var timeElapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
    var hasPassed = ret.first;
    var failureOutput = ret.second;
    if (!expectedToPass && !hasPassed) {
      if (shouldPrint) {
        printFailedSuccessfully(pairIndex,
                                numTests,
                                sourceFilePath,
                                timeElapsed,
                                failureOutput
        );
      }
      return true;
    } else if (expectedToPass && hasPassed) {
      if (shouldPrint) {
        printSuccessError(pairIndex,
                          numTests,
                          sourceFilePath,
                          timeElapsed
        );
      }
      return true;
    } else {
      if (shouldPrint) {
        printFailError(pairIndex,
                       numTests,
                       sourceFilePath,
                       timeElapsed,
                       failureOutput
        );
      }
      return false;
    }
  }

  public static BiFunction<String, String, Pair<Boolean, String>>
  getTestFunctionForType(@NotNull Type type) {
    return switch (type) {
      case SCANNER -> TestRunner::scannerTestFunction;
      case PARSER -> TestRunner::parserTestFunction;
      case SEMANTICS -> TestRunner::semanticsTestFunction;
    };
  }

  public static String getTestsFilePathForType(@NotNull Type type) {
    return switch (type) {
      case SCANNER -> "tests/scanner";
      case PARSER -> "tests/parser";
      case SEMANTICS -> "tests/semantics";
    };
  }

  public enum Type {
    SCANNER,
    PARSER,
    SEMANTICS,
  }
}
