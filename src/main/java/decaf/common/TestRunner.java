package decaf.common;

import static java.util.logging.Level.SEVERE;
import static decaf.common.Utils.DECAF_ASCII_ART;

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
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import decaf.grammar.Parser;
import decaf.grammar.Scanner;
import decaf.ir.SemanticChecker;

public class TestRunner {
  public static final String DEFAULT_DATAFLOW_TESTS_ROOT = "tests/optimizer/dcf";
  private static final Logger logger = Logger.getLogger(TestRunner.class.getCanonicalName());

  public static void run() {
    try {
      compileTests();
    } catch (IOException e) {
      logger.log(
          SEVERE,
          "Failed to compile tests due to an IO error",
          e
      );
    }
  }

  private static String readFile(File file) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(file));
    return reader.lines()
                 .collect(Collectors.joining(System.lineSeparator()));
  }

  private static List<File> getAllTestFiles(String filepath) {
    try (var files = Files.list(Paths.get(filepath))) {
      return files.map(Path::toFile)
                  .filter(File::isFile)
                  .sorted(Comparator.comparing(File::getName))
                  .toList();
    } catch (IOException e) {
      logger.log(
          SEVERE,
          "Failed to compile tests due to an IO error",
          e
      );
      return Collections.emptyList();
    }
  }

  public static void testCodegen() {
    final var filepath = "tests/codegen/input";
    var allTestFiles = getAllTestFiles(filepath);

    int nPassed = 0;
    for (var file : allTestFiles) {
      Stopwatch stopwatch = Stopwatch.createStarted();
      System.out.print("compiling... " + file.getName());
      try {
        var compilation = compileTest(file);
        var expectedOutputFile = "./tests/codegen/output/" + file.getName() + ".out";
        var expected = readFile(Paths.get(expectedOutputFile)
                                     .toFile());
        System.out.println(
            "\rcompiled... " + file.getName() + " in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
        if (!expected.strip()
                     .equals(compilation.output.strip())) {
          System.out.println(Utils.coloredPrint(
              "\tFAIL: " + file.getName(),
              Utils.ANSIColorConstants.ANSI_RED
          ));
          System.out.println("EXPECTED:");
          System.out.println(expected);
          System.out.println("RECEIVED:");
          System.out.println(compilation.output);
        } else {
          System.out.println(Utils.coloredPrint(
              "\tPASSED: " + file.getName(),
              Utils.ANSIColorConstants.ANSI_GREEN_BOLD
          ));
          nPassed += 1;
        }
      } catch (Exception e) {
        logger.log(
            SEVERE,
            "Failed to run tests",
            e
        );
      }
    }
    System.out.format(
        "Passed %d out of %d\n",
        nPassed,
        allTestFiles.size()
    );
  }

  public static boolean testCodegenSingleFile(String filename) {
    final var filepath = "tests/codegen/input/";
    var file = Paths.get(filepath + filename)
                    .toFile();
    Stopwatch stopwatch = Stopwatch.createStarted();
    System.out.print("compiling... " + file.getName());
    try {
      var compilation = compileTest(file);
      var expectedOutputFile = "./tests/codegen/output/" + file.getName() + ".out";
      var expected = readFile(Paths.get(expectedOutputFile)
                                   .toFile());
      System.out.println("\rcompiled... " + file.getName() + " in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " ms");
      if (!expected.strip()
                   .equals(compilation.output.strip())) {
        System.out.println(Utils.coloredPrint(
            "\tFAIL: " + file.getName(),
            Utils.ANSIColorConstants.ANSI_RED
        ));
        System.out.println("EXPECTED:");
        System.out.println(expected);
        System.out.println("RECEIVED:");
        System.out.println(compilation.output);
      } else {
        System.out.println(Utils.coloredPrint(
            "\tPASSED: " + file.getName(),
            Utils.ANSIColorConstants.ANSI_GREEN_BOLD
        ));
        return true;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
    return false;
  }

  private static void compileTests() throws IOException {
    var allTestFiles = getAllTestFiles(TestRunner.DEFAULT_DATAFLOW_TESTS_ROOT);
    var nTestFiles = allTestFiles.size();

    String[] fileNames = new String[nTestFiles];
    double[] reductionRatios = new double[nTestFiles];
    int[] nLinesRemoved = new int[nTestFiles];
    for (var indexOfTestFile = 0; indexOfTestFile < nTestFiles; indexOfTestFile++) {
      var testFile = allTestFiles.get(indexOfTestFile);
      if (testFile.getName()
                  .equals("test.dcf"))
        continue;
      System.out.println(testFile.getAbsolutePath());
      var compilation = compileTest(testFile);
      fileNames[indexOfTestFile] = testFile.getName();
      reductionRatios[indexOfTestFile] = compilation.getNLinesOfCodeReductionFactor();
      nLinesRemoved[indexOfTestFile] = compilation.getNLinesRemovedByAssemblyOptimizer();
    }
    System.out.format(
        "%10s\t%20s\t%15s\t%15s\n",
        "INDEX",
        "FILE NAME",
        "REDUCTION RATIO",
        "#ASM LINES REMOVED"
    );
    for (int i = 0; i < nTestFiles; i++) {
      System.out.format(
          "%10d\t%20s\t%10.4f%%\t%10d\n",
          i,
          fileNames[i],
          reductionRatios[i] * 100,
          nLinesRemoved[i]
      );
    }
    var doubleSummaryStatistics = DoubleStream.of(reductionRatios)
                                              .summaryStatistics();
    System.out.format(
        "\t%s\t%10.4f%%\n",
        "AVERAGE REDUCTION",
        doubleSummaryStatistics.getAverage() * 100
    );
    System.out.format(
        "\t%s\t%10d\n",
        "TOTAL #ASM LINES REMOVED REDUCTION",
        IntStream.of(nLinesRemoved)
                 .summaryStatistics()
                 .getSum()
    );

  }

  private static Compilation compileTest(File testFile) throws IOException {
    var compilation = new Compilation(
        readFile(testFile),
        true
    );
    compilation.run();
    return compilation;
  }

  private static @NotNull List<Pair<File, File>> getTestPairs(
      @NotNull String filepathPrefix,
      boolean expectedToPas
  ) {
    return getTestPairs(
        filepathPrefix,
        expectedToPas,
        true
    );
  }

  private static @NotNull List<Pair<File, File>> getTestPairs(
      @NotNull String filepathPrefix,
      boolean expectedToPass,
      boolean outputExists
  ) {
    var filepath = filepathPrefix + "/input/" + (expectedToPass ? "/legal": "/illegal");
    try (var files = Files.list(Paths.get(filepath))) {
      if (outputExists) {
        return files.map(Path::toFile)
                    .filter(File::isFile)
                    .sorted(Comparator.comparing(File::getName))
                    .map(file -> new Pair<>(
                        file,
                        Paths.get(filepathPrefix + "/output/" + file.getName()
                                                                    .substring(
                                                                        0,
                                                                        file.getName()
                                                                            .length() - 4
                                                                    ) + ".out")
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
      int pairIndex,
      int numTests,
      String filePath,
      long runTimeMs,
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
      int pairIndex,
      int numTests,
      String filePath,
      long runTimeMs
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
      int pairIndex,
      int numTests,
      String filePath,
      long runTimeMs,
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

  private static String getTestingPrologue(Type type) {
    switch (type) {
      case SCANNER -> {
        return String.format(
            "%s\n%s\n",
            DECAF_ASCII_ART,
            "Testing Scanner"
        );
      }
      case PARSER -> {
        return "Testing Parser";
      }
      case AST -> {
        return "Testing AST";
      }
      case SEMANTICS -> {
        return "Testing Semantics";
      }
      case IR -> {
        return "Testing IR";
      }
      case CODEGEN -> {
        return "Testing Codegen";
      }
      case OPTIMIZER -> {
        return "Testing Optimizer";
      }
      default -> throw new IllegalArgumentException("Unknown test type");

    }
  }

  private static String getTestingEpilogue(
      Type type,
      String filePath,
      long totalTimeElapsed,
      List<Quadruple<String, Integer, Integer, Long>> testSuiteResults
  ) {
    var strings = new ArrayList<String>();
    strings.add(
        "Test Suites Summary"
    );
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
        strings.add(
            String.format(
                "\t\t%s: %s",
                testName,
                Utils.coloredPrint(
                    String.format(
                        "✓ Passed all %s tests in %s ms",
                        numTests,
                        timeElapsed
                    ),
                    Utils.ANSIColorConstants.ANSI_GREEN_BOLD
                )
            ));
      } else {
        strings.add(
            Utils.coloredPrint(
                String.format(
                    "\t\t%s: ✗ Passed %s out of %s tests in %s ms",
                    testName,
                    numPassed,
                    numTests,
                    timeElapsed
                ),
                Utils.ANSIColorConstants.ANSI_RED
            )
        );
      }

    }
    strings.add(String.format(
        "%s %s",
        (allTestsNumPassed == allTestsNumTests) ? Utils.coloredPrint(
            "PASS",
            Utils.ANSIColorConstants.ANSI_BG_GREEN
        ): Utils.coloredPrint(
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
              String.format(
                  "✓ Passed all %s tests in %s ms",
                  allTestsNumTests,
                  totalTimeElapsed
              ),
              Utils.ANSIColorConstants.ANSI_GREEN_BOLD
          )
      );
    } else {
      strings.add(
          Utils.coloredPrint(
              String.format(
                  "✗ Passed %s out of %s tests in %s ms",
                  allTestsNumPassed,
                  allTestsNumTests,
                  totalTimeElapsed
              ),
              Utils.ANSIColorConstants.ANSI_RED
          )
      );
    }
    return String.join(
        "\n",
        strings
    );
  }

  private static String formatTokensToOutputFormat(@NotNull Scanner scanner) {
    var strings = new ArrayList<String>();
    for (var token : scanner) {
      var text = switch (token.tokenType) {
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

  public static void testScanner(
      @NotNull String filename,
      boolean expectedToPass
  ) {
    final String filepath = "tests/scanner";
    var input = Paths.get(filepath + "/input/" + (expectedToPass ? "legal/": "illegal/") + filename + ".dcf");
    var output = Paths.get(filepath + "/output/" + filename + ".out");
    var testPairs = List.of(
        new Pair<>(
            input.toFile(),
            output.toFile()
        )
    );
    testScanner(
        testPairs,
        true
    );
  }

  public static void testSemantics(
      @NotNull String filename,
      boolean expectedToPass
  ) {
    final String filepath = "tests/semantics";
    var input = Paths.get(filepath + "/input/" + (expectedToPass ? "legal/": "illegal/") + filename + ".dcf").toFile();
    var testPairs = List.of(
        new Pair<>(
            input,
            input
        )
    );
    testSemantics(
        testPairs,
        expectedToPass
    );
  }

  public static void testParser(
      @NotNull String filename,
      boolean expectedToPass
  ) {
    final String filepath = "tests/parser";
    var input = Paths.get(filepath + "/input/" + (expectedToPass ? "legal/": "illegal/") + filename + ".dcf")
                     .toFile();
    var testPairs = List.of(
        new Pair<>(
            input,
            input
        )
    );
    testParser(
        testPairs,
        true
    );
  }

  public static void testScanner() {
    final String filepath = "tests/scanner";

    System.out.println(getTestingPrologue(Type.SCANNER));

    var startTime = Stopwatch.createStarted();
    var expectedToPassTestPairs = getTestPairs(
        filepath,
        true
    );
    var expectedToPassResults = testScanner(
        expectedToPassTestPairs,
        true
    );

    var expectedToFailTestPairs = getTestPairs(
        filepath,
        false
    );
    var expectedToFailResults = testScanner(
        expectedToFailTestPairs,
        false
    );

    System.out.println(getTestingEpilogue(
        Type.SCANNER,
        "tests/scanner",
        startTime.elapsed(TimeUnit.MILLISECONDS),
        List.of(
            new Quadruple<>(
                "legal",
                expectedToPassResults.first(),
                expectedToPassTestPairs.size(),
                expectedToPassResults.second()
            ),
            new Quadruple<>(
                "illegal",
                expectedToFailResults.first(),
                expectedToFailTestPairs.size(),
                expectedToFailResults.second()
            )
        )
    ));
  }

  public static void testSemantics() {
    final String filepath = "tests/semantics";

    System.out.println(getTestingPrologue(Type.SEMANTICS));

    var startTime = Stopwatch.createStarted();
    var expectedToPassTestPairs = getTestPairs(
        filepath,
        true,
        false
    );
    var expectedToPassResults = testSemantics(
        expectedToPassTestPairs,
        true
    );

    var expectedToFailTestPairs = getTestPairs(
        filepath,
        false,
        false
    );
    var expectedToFailResults = testSemantics(
        expectedToFailTestPairs,
        false
    );

    System.out.println(getTestingEpilogue(
        Type.SEMANTICS,
        "tests/semantics",
        startTime.elapsed(TimeUnit.MILLISECONDS),
        List.of(
            new Quadruple<>(
                "legal",
                expectedToPassResults.first(),
                expectedToPassTestPairs.size(),
                expectedToPassResults.second()
            ),
            new Quadruple<>(
                "illegal",
                expectedToFailResults.first(),
                expectedToFailTestPairs.size(),
                expectedToFailResults.second()
            )
        )
    ));
  }

  public static Pair<Integer, Long> testSuite(
      List<Pair<File, File>> testPairs,
      boolean expectedToPass,
      BiFunction<String, String, Pair<Boolean, String>> fn
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
        numPassed += testOne(
            fn,
            sourceCode,
            expectedOutput,
            pairIndex,
            numTests,
            sourceFile.getAbsolutePath(),
            expectedToPass
        ) ? 1: 0;
      } catch (IOException e) {
        printFailError(
            pairIndex,
            numTests,
            sourceFile.getAbsolutePath(),
            stopwatch.elapsed(TimeUnit.MILLISECONDS),
            e.getMessage()
        );
      }
    }
    return new Pair<>(
        numPassed,
        testSuiteTestTime.elapsed(TimeUnit.MILLISECONDS)
    );
  }

  public static Pair<Integer, Long> testScanner(
      List<Pair<File, File>> testPairs,
      boolean expectedToPass
  ) {
    BiFunction<String, String, Pair<Boolean, String>> fn = (String input, String output) -> {
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
    };
    return testSuite(
        testPairs,
        expectedToPass,
        fn
    );
  }

  public static Pair<Integer, Long> testParser(
      List<Pair<File, File>> testPairs,
      boolean expectedToPass
  ) {
    BiFunction<String, String, Pair<Boolean, String>> fn = (String input, String output) -> {
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
    };
    return testSuite(
        testPairs,
        expectedToPass,
        fn
    );
  }

  public static Pair<Integer, Long> testSemantics(
      List<Pair<File, File>> testPairs,
      boolean expectedToPass
  ) {
    BiFunction<String, String, Pair<Boolean, String>> fn = (String input, String output) -> {
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
      var semanticChecker = new SemanticChecker(parser.getRoot(), context);
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
    };
    return testSuite(
        testPairs,
        expectedToPass,
        fn
    );
  }

  public static void testParser() {
    final String filepath = "tests/parser";

    System.out.println(getTestingPrologue(Type.PARSER));
    var startTime = Stopwatch.createStarted();
    var expectedToPassTestPairs = getTestPairs(
        filepath,
        true,
        false
    );
    var expectedToPassResults = testParser(
        expectedToPassTestPairs,
        true
    );

    var expectedToFailTestPairs = getTestPairs(
        filepath,
        false,
        false
    );
    var expectedToFailResults = testParser(
        expectedToFailTestPairs,
        false
    );

    System.out.println(getTestingEpilogue(
        Type.PARSER,
        "tests/parser",
        startTime.elapsed(TimeUnit.MILLISECONDS),
        List.of(
            new Quadruple<>(
                "legal",
                expectedToPassResults.first(),
                expectedToPassTestPairs.size(),
                expectedToPassResults.second()
            ),
            new Quadruple<>(
                "illegal",
                expectedToFailResults.first(),
                expectedToFailTestPairs.size(),
                expectedToFailResults.second()
            )
        )
    ));
  }

  private static <InputType, OutputType> boolean testOne(
      @NotNull BiFunction<InputType, OutputType, Pair<Boolean, String>> fn,
      @NotNull InputType input,
      @Nullable OutputType expectedOutput,
      int pairIndex,
      int numTests,
      @Nullable String sourceFilePath,
      boolean expectedToPass
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
      printFailedSuccessfully(
          pairIndex,
          numTests,
          sourceFilePath,
          timeElapsed,
          failureOutput
      );
      return true;
    } else if (expectedToPass && hasPassed) {
      printSuccessError(
          pairIndex,
          numTests,
          sourceFilePath,
          timeElapsed
      );
      return true;
    } else {
      printFailError(
          pairIndex,
          numTests,
          sourceFilePath,
          timeElapsed,
          failureOutput
      );
      return false;
    }
  }

  public enum Type {
    SCANNER,
    PARSER,
    AST,
    SEMANTICS,
    IR,
    CODEGEN,
    OPTIMIZER,
  }
}
