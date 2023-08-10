package decaf.common;

import static java.util.logging.Level.SEVERE;
import static decaf.common.Utils.DECAF_ASCII_ART;

import com.google.common.base.Stopwatch;

import org.jetbrains.annotations.NotNull;

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
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import decaf.grammar.Scanner;
import decaf.grammar.Token;

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
        CompilationContext.isDebugModeOn()
    );
    compilation.run();
    return compilation;
  }

  private static @NotNull List<Pair<File, File>> getTestPairs(@NotNull String filepath) {
    try (var files = Files.list(Paths.get(filepath + "/input"))) {
      return files.map(Path::toFile)
                  .filter(File::isFile)
                  .sorted(Comparator.comparing(File::getName))
                  .map(file -> new Pair<>(
                      file,
                      Paths.get(filepath + "/output/" + file.getName()
                                                            .substring(
                                                                0,
                                                                file.getName()
                                                                    .length() - 4
                                                            ) + ".out")
                           .toFile()
                  ))
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

  private static void printFailError(
      int pairIndex,
      int numTests,
      String filePath,
      String reason,
      long runTimeMs
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
      case CFG -> {
        return "Testing CFG";
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
      default -> {
        throw new IllegalArgumentException("Unknown test type");
      }

    }
  }

  private static String getTestingEpilogue(
      Type type,
      int numTests,
      String filePath,
      int numPassed,
      long timeElapsed
  ) {
    var strings = new ArrayList<String>();
    strings.add(String.format(
        "%s %s",
        (numPassed == numTests) ? Utils.coloredPrint(
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
    if (numTests == numPassed) {
      strings.add(
          Utils.coloredPrint(
              String.format(
                  "✓ Passed all %s tests in %s ms",
                  numTests,
                  timeElapsed
              ),
              Utils.ANSIColorConstants.ANSI_GREEN_BOLD
          )
      );
    } else {
      strings.add(
          Utils.coloredPrint(
              String.format(
                  "✗ Passed %s out of %s tests in %s ms",
                  numPassed,
                  numTests,
                  timeElapsed
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


  public static void testScanner(@NotNull String filename) {
    final String filepath = "tests/scanner";
    var input = Paths.get(filepath + "/input/" + filename + ".dcf");
    var output = Paths.get(filepath + "/output/" + filename + ".out");
    var testPairs = List.of(
        new Pair<>(
            input.toFile(),
            output.toFile()
        )
    );
    testScanner(testPairs);
  }

  public static void testScanner() {
    final String filepath = "tests/scanner";
    var testPairs = getTestPairs(filepath);
    testScanner(testPairs);
  }

  public static void testScanner(List<Pair<File, File>> testPairs) {
    System.out.println(getTestingPrologue(Type.SCANNER));
    final int numTests = testPairs.size();
    int numPassed = 0;
    var startTime = Stopwatch.createStarted();
    for (var pairIndex = 0; pairIndex < testPairs.size(); pairIndex++) {
      Stopwatch stopwatch = Stopwatch.createStarted();
      var pair = testPairs.get(pairIndex);
      var sourceFile = pair.first;
      try {
        var expectedOutput = readFile(pair.second);
        int headerEndIndex = expectedOutput.indexOf("\n");
        var header = expectedOutput.substring(
            0,
            headerEndIndex
        );
        var expectedOutputBody = expectedOutput.substring(headerEndIndex + 1);
        var scanner = new Scanner(
            readFile(pair.first),
            new DecafExceptionProcessor(readFile(pair.first))
        );
        var actualOutput = formatTokensToOutputFormat(scanner);
        if (scanner.finished()) {
          if (!actualOutput.equals(expectedOutputBody)) {
            printFailError(
                pairIndex,
                numTests,
                sourceFile.getAbsolutePath(),
                actualOutput,
                stopwatch.elapsed(TimeUnit.MILLISECONDS)
            );
          } else {
            printSuccessError(
                pairIndex,
                numTests,
                sourceFile.getAbsolutePath(),
                stopwatch.elapsed(TimeUnit.MILLISECONDS)
            );
            numPassed += 1;
          }
        } else {
          if (header.equals("PASS")) {
            printFailError(
                pairIndex,
                numTests,
                sourceFile.getAbsolutePath(),
                scanner.getPrettyErrorOutput(),
                stopwatch.elapsed(TimeUnit.MILLISECONDS)
            );
          } else {
            printFailedSuccessfully(
                pairIndex,
                numTests,
                sourceFile.getAbsolutePath(),
                stopwatch.elapsed(TimeUnit.MILLISECONDS),
                scanner.getPrettyErrorOutput()
            );
            numPassed += 1;
          }
        }
      } catch (IOException e) {
        printFailError(
            pairIndex,
            numTests,
            sourceFile.getAbsolutePath(),
            e.getMessage(),
            stopwatch.elapsed(TimeUnit.MILLISECONDS)
        );
      }
    }
    System.out.println(getTestingEpilogue(
        Type.SCANNER,
        numTests,
        "tests/scanner",
        numPassed,
        startTime.elapsed(TimeUnit.MILLISECONDS)
    ));
  }

  private static String formatTokensToOutputFormat(@NotNull Scanner scanner) {
    var strings = new ArrayList<String>();
    Token token;
    for (token = scanner.nextToken(); token.isNotEOF(); token = scanner.nextToken()) {
      String text;
      switch (token.tokenType()) {
        case ID -> text = "IDENTIFIER" + " " + token.lexeme();
        case STRING_LITERAL -> text = "STRINGLITERAL" + " " + token.lexeme();
        case CHAR_LITERAL -> text = "CHARLITERAL" + " " + token.lexeme();
        case HEX_LITERAL, DECIMAL_LITERAL -> text = "INTLITERAL" + " " + token.lexeme();
        case RESERVED_FALSE, RESERVED_TRUE -> text = "BOOLEANLITERAL" + " " + token.lexeme();
        default -> text = token.lexeme();
      }
      strings.add(token.tokenPosition()
                       .line() + 1 + " " + text);
    }
    return String.join(
        "\n",
        strings
    );
  }

  public enum Type {
    SCANNER,
    PARSER,
    AST,
    CFG,
    IR,
    CODEGEN,
    OPTIMIZER,
  }
}
