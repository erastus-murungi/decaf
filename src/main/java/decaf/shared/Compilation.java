package decaf.shared;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import decaf.analysis.lexical.Scanner;
import decaf.analysis.semantic.SemanticChecker;
import decaf.analysis.syntax.Parser;

public class Compilation {
  private final static String osName =
      System.getProperty("os.name")
            .replaceAll(
                "\\s",
                ""
            )
            .toLowerCase(
                Locale.ROOT);
  private static final Logger logger =
      Logger.getLogger(Compilation.class.getName());
  private final CompilationContext compilationContext;
  String output = null;
  private String sourceCode;
  private Scanner scanner;
  private Parser parser;
  private SemanticChecker semanticChecker;
  private PrintStream outputStream;
  private CompilationState compilationState;
  private double nLinesOfCodeReductionFactor = 0.0D;

  public Compilation(
      String filenameOrSourceCode, boolean debug,
      boolean isFilename
  ) throws FileNotFoundException {
    if (isFilename) {
      compilationContext =
          specificTestFileInitialize(new FileInputStream(filenameOrSourceCode));
    } else {
      compilationContext = new CompilationContext(filenameOrSourceCode);
    }
    initialize();
    compilationContext.setDebugMode(debug);
  }

  public Compilation(String filename, boolean debug)
      throws FileNotFoundException {
    this(
        filename,
        debug,
        false
    );
  }

  public int getNLinesRemovedByAssemblyOptimizer() {
    return 0;
  }

  public double getNLinesOfCodeReductionFactor() {
    return nLinesOfCodeReductionFactor;
  }

  private void runNextStep() {
    switch (compilationState) {
      case INITIALIZED -> System.out.println("starting!");
      case SCANNED -> runParser();
      case PARSED -> runSemanticsChecker();
      default -> throw new IllegalStateException("Unexpected value: " + compilationState);
    }
  }

  private void compileAssembly() {
    assert compilationState == CompilationState.ASSEMBLED;
    if (osName.equals("macosx")) {
      try {
        var process = Runtime.getRuntime()
                             .exec("clang " + "/Users/erastusmurungi/IdeaProjects/compiler/test.s" +
                                       " -mllvm --x86-asm-syntax=att -o main");
        process.waitFor();
        if (compilationContext.debugModeOn()) {
          System.out.println(Utils.getStringFromInputStream(
              process.getErrorStream(),
              logger
          ));
          System.out.println(Utils.getStringFromInputStream(
              process.getInputStream(),
              logger
          ));
        }
      } catch (IOException e) {
        logger.log(
            Level.SEVERE,
            "error compiling assembly due to IO error: %s",
            e
        );
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
      try {
        var process = Runtime.getRuntime()
                             .exec("/Users/erastusmurungi/IdeaProjects/compiler/main");
        process.waitFor(
            60,
            TimeUnit.SECONDS
        );
        if (process.exitValue() == 0 || process.isAlive()) output =
            Utils.getStringFromInputStream(
                process.getErrorStream(),
                logger
            ) +
                Utils.getStringFromInputStream(
                    process.getInputStream(),
                    logger
                );
        else output = "TIMEOUT";
        if (compilationContext.debugModeOn()) System.out.println(output);
      } catch (IOException e) {
        logger.log(
            Level.SEVERE,
            "error running compiled assembly due to IO error: %s",
            e
        );
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new IllegalStateException("only darwin platform is supported not : " + osName);
    }
    compilationState = CompilationState.COMPLETED;
  }

  public void run() {
    runScanner();
    while (compilationState != CompilationState.COMPLETED) {
      runNextStep();
    }
  }

  private CompilationContext specificTestFileInitialize(InputStream inputStream) {
    sourceCode = Utils.getStringFromInputStream(
        inputStream,
        logger
    );
    var compilationContext = new CompilationContext(sourceCode);
    compilationContext.setDebugMode(true);
    return compilationContext;
  }

  private void initialize() {
    outputStream = System.out;
    compilationState = CompilationState.INITIALIZED;
  }

  private void runScanner() {
    assert compilationState == CompilationState.INITIALIZED;
    scanner = new Scanner(
        sourceCode,
        compilationContext
    );
    scanner.setTrace(compilationContext.debugModeOn());
    compilationState = CompilationState.SCANNED;
  }

  private void runParser() {
    assert compilationState == CompilationState.SCANNED;
    parser = new Parser(
        scanner,
        compilationContext
    );

    if (parser.hasError()) {
      System.out.println(parser.getPrettyErrorOutput());
      System.exit(1);
    }
    compilationState = CompilationState.PARSED;
    if (compilationContext.debugModeOn()) System.out.println(parser.getRoot()
                                                                   .getSourceCode());
  }

  private void runSemanticsChecker() {
    assert compilationState == CompilationState.PARSED;
    semanticChecker = new SemanticChecker(
        parser.getRoot(),
        compilationContext
    );
    if (semanticChecker.hasErrors()) {
      System.exit(1);
    }
    compilationState = CompilationState.SEM_CHECKED;
  }

  enum CompilationState {
    INITIALIZED, SCANNED, PARSED, SEM_CHECKED, CFG_GENERATED, SSA_GENERATED, IR_GENERATED, DATAFLOW_OPTIMIZED, ASSEMBLED, COMPLETED
  }
}
