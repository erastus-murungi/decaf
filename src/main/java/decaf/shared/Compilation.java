package decaf.shared;

import decaf.analysis.lexical.Scanner;
import decaf.analysis.semantic.SemanticChecker;
import decaf.analysis.syntax.Parser;
import decaf.analysis.cfg.Cfg;

import java.io.*;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Compilation {
    private final static String osName = System.getProperty("os.name").replaceAll("\\s", "").toLowerCase(Locale.ROOT);
    private static final Logger logger = Logger.getLogger(Compilation.class.getName());
    private final CompilationContext compilationContext;
    String output = null;
    private Scanner scanner;
    private Parser parser;
    private SemanticChecker semanticChecker;
    private PrintStream outputStream;
    private CompilationState compilationState;

    private Compilation(String filenameOrSourceCode, boolean debug, boolean isFilename) throws FileNotFoundException {
        if (isFilename) {
            compilationContext = specificTestFileInitialize(new FileInputStream(filenameOrSourceCode));
        } else {
            compilationContext = CompilationContext.fromSourceCode(filenameOrSourceCode, debug);
        }
        initialize();
        compilationContext.setDebugMode(debug);
    }

    public static Compilation forSourceCode(String sourceCode, boolean debug) throws FileNotFoundException {
        return new Compilation(sourceCode, debug, false);
    }

    public static Compilation forSourceCode(String sourceCode) throws FileNotFoundException {
        return new Compilation(sourceCode, false, false);
    }

    public static Compilation forTestFile(String filename, boolean debug) throws FileNotFoundException {
        return new Compilation(filename, debug, true);
    }

    public static Compilation forTestFile(String filename) throws FileNotFoundException {
        return new Compilation(filename, false, true);
    }

    private void runNextStep() {
        switch (compilationState) {
            case INITIALIZED -> System.out.println("starting!");
            case SCANNED -> runParser();
            case PARSED -> runSemanticsChecker();
            case SEM_CHECKED -> createSourceLevelCFGs();
            default -> {
              if (compilationContext.debugModeOn()) {
                System.out.println("compilation completed!");
              }
                System.out.println(compilationContext.getEntryCfgBlock("main"));
                compilationState = CompilationState.COMPLETED;
            }
        }
    }

    private void compileAssembly() {
        assert compilationState == CompilationState.ASSEMBLED;
        if (osName.equals("macosx")) {
            try {
                var process = new ProcessBuilder("clang",
                                                 "/Users/erastusmurungi/IdeaProjects/compiler/test.s",
                                                 "-mllvm --x86-asm-syntax=att -o main"
                ).start();
                process.waitFor();
                if (compilationContext.debugModeOn()) {
                    System.out.println(Utils.getStringFromInputStream(process.getErrorStream(), logger));
                    System.out.println(Utils.getStringFromInputStream(process.getInputStream(), logger));
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "error compiling assembly due to IO error: %s", e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            try {
                var process = new ProcessBuilder("/Users/erastusmurungi/IdeaProjects/compiler/main").start();
                process.waitFor(60, TimeUnit.SECONDS);
              if (process.exitValue() == 0 || process.isAlive()) {
                output = Utils.getStringFromInputStream(process.getErrorStream(), logger) +
                         Utils.getStringFromInputStream(process.getInputStream(), logger);
              } else {
                output = "TIMEOUT";
              }
              if (compilationContext.debugModeOn()) {
                System.out.println(output);
              }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "error running compiled assembly due to IO error: %s", e);
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
        var sourceCode = Utils.getStringFromInputStream(inputStream, logger);
        var compilationContext = CompilationContext.fromSourceCode(sourceCode);
        compilationContext.setDebugMode(true);
        return compilationContext;
    }

    private void initialize() {
        outputStream = System.out;
        compilationState = CompilationState.INITIALIZED;
    }

    private void runScanner() {
        assert compilationState == CompilationState.INITIALIZED;
        scanner = new Scanner(compilationContext);
        if (!compilationContext.scanningSuccessful()) {
            System.exit(1);
        }
        compilationState = CompilationState.SCANNED;
    }

    private void runParser() {
        assert compilationState == CompilationState.SCANNED;
        parser = new Parser(scanner, compilationContext);

        if (!compilationContext.parsingSuccessful()) {
            System.exit(1);
        }
        compilationState = CompilationState.PARSED;
      if (compilationContext.debugModeOn()) {
        System.out.println(parser.getRoot().getSourceCode());
      }
    }

    private void runSemanticsChecker() {
        assert compilationState == CompilationState.PARSED;
        semanticChecker = new SemanticChecker(parser.getRoot(), compilationContext);
        if (compilationContext.semanticCheckingUnsuccessful()) {
            System.exit(1);
        }
        compilationState = CompilationState.SEM_CHECKED;
    }

    private void createSourceLevelCFGs() {
        assert compilationState == CompilationState.SEM_CHECKED;
        Cfg.build(compilationContext);
        compilationState = CompilationState.CFG_GENERATED;
    }

    enum CompilationState {
        INITIALIZED, SCANNED, PARSED, SEM_CHECKED, CFG_GENERATED, SSA_GENERATED, IR_GENERATED, DATAFLOW_OPTIMIZED, ASSEMBLED, COMPLETED
    }
}
