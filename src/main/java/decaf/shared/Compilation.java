package decaf.shared;

import decaf.synthesis.asm.X86AsmWriter;
import decaf.ir.cfg.ControlFlowGraph;
import decaf.ir.BasicBlockToInstructionListConverter;
import decaf.ir.dataflow.DataflowOptimizer;
import decaf.ir.dataflow.passes.InstructionSimplifyIrPass;
import decaf.analysis.syntax.Parser;
import decaf.analysis.lexical.Scanner;
import decaf.analysis.semantic.SemanticChecker;
import decaf.synthesis.regalloc.RegisterAllocator;
import decaf.ir.ssa.SSA;
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

public class Compilation {
  private final static String osName =
      System.getProperty("os.name").replaceAll("\\s", "").toLowerCase(
          Locale.ROOT);
  private static final Logger logger =
      Logger.getLogger(Compilation.class.getName());
  private final CompilationContext compilationContext;
  String output = null;
  private String sourceCode;
  private Scanner scanner;
  private Parser parser;
  private SemanticChecker semanticChecker;
  private ControlFlowGraph cfg;
  private BasicBlockToInstructionListConverter
      basicBlockToInstructionListConverter;
  private ProgramIr programIr;
  private PrintStream outputStream;
  private CompilationState compilationState;
  private double nLinesOfCodeReductionFactor = 0.0D;

  public Compilation(String filenameOrSourceCode, boolean debug,
                     boolean isFilename) throws FileNotFoundException {
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
    this(filename, debug, false);
  }

  public int getNLinesRemovedByAssemblyOptimizer() { return 0; }

  public double getNLinesOfCodeReductionFactor() {
    return nLinesOfCodeReductionFactor;
  }

  private void runNextStep() {
    switch (compilationState) {
      case INITIALIZED -> System.out.println("starting!");
      case SCANNED -> runParser();
      case PARSED -> runSemanticsChecker();
      case SEM_CHECKED -> generateCFGs();
      case CFG_GENERATED -> generateIr();
      case IR_GENERATED -> generateSsa();
      case SSA_GENERATED -> runDataflowOptimizationPasses();
      case DATAFLOW_OPTIMIZED -> generateAssembly();
      case ASSEMBLED -> compileAssembly();
      case COMPLETED -> System.out.println("done!");
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
    semanticChecker = new SemanticChecker(parser.getRoot(), compilationContext);
    if (semanticChecker.hasErrors()) {
      System.exit(1);
    }
    compilationState = CompilationState.SEM_CHECKED;
  }

  private void generateCFGVisualizationPdfs() {
    var copy = new HashMap<>(cfg.getMethodNameToEntryBlockMapping());
    copy.put(
        "globals",
        cfg.getPrologueBasicBlock()
    );
//        GraphVizPrinter.printGraph(copy, (basicBlock -> basicBlock.threeAddressCodeList.getCodes().stream().map(ThreeAddressCode::repr).collect(Collectors.joining("\n"))));
    GraphVizManager.printGraph(copy);
  }

  private boolean shouldOptimize() {
    return true;
  }

  private void generateSymbolTablePdfs() {
    GraphVizManager.printSymbolTables(
        parser.getRoot(),
        basicBlockToInstructionListConverter.getPerMethodSymbolTables()
    );
  }

  private void generateCFGs() {
    assert compilationState == CompilationState.SEM_CHECKED;
    if (shouldOptimize()) {

      if (compilationContext.debugModeOn()) {
        System.out.println("before InstructionSimplifyPass");
        System.out.println(parser.getRoot()
                                 .getSourceCode());
      }

      InstructionSimplifyIrPass.run(parser.getRoot());
      if (compilationContext.debugModeOn()) {
        System.out.println("after InstructionSimplifyPass");
        System.out.println(parser.getRoot()
                                 .getSourceCode());
      }
    }

    cfg = new ControlFlowGraph(
        parser.getRoot(),
        semanticChecker.getGlobalDescriptor()
    );
    cfg.build();
    compilationState = CompilationState.CFG_GENERATED;
  }

  private void generateIr() {
    assert compilationState == CompilationState.CFG_GENERATED;
    basicBlockToInstructionListConverter = new BasicBlockToInstructionListConverter(cfg);
    programIr = basicBlockToInstructionListConverter.getProgramIr();
    if (compilationContext.debugModeOn()) {
      generateSymbolTablePdfs();
    }
    if (compilationContext.debugModeOn()) {
      generateCFGVisualizationPdfs();
    }
    compilationState = CompilationState.IR_GENERATED;
    if (compilationContext.debugModeOn()) {
      System.out.println(programIr.mergeProgram());
    }
  }

  private int countLinesOfCode() {
    return programIr.toSingleInstructionList()
                    .size();
  }

  private void generateSsa() {
    assert compilationState == CompilationState.IR_GENERATED;
    programIr.getMethods()
             .forEach(SSA::construct);
    compilationState = CompilationState.SSA_GENERATED;
  }

  private void runDataflowOptimizationPasses() {
    assert compilationState == CompilationState.SSA_GENERATED;
    double oldNLinesOfCode;
    oldNLinesOfCode = countLinesOfCode();

    if (shouldOptimize()) {
      if (compilationContext.debugModeOn()) {
        System.out.println("Before optimization");
        System.out.println(programIr.mergeProgram());
      }
      programIr.setGlobals(basicBlockToInstructionListConverter.getGlobalNames());
      var dataflowOptimizer = new DataflowOptimizer(
          programIr,
          compilationContext
      );
      dataflowOptimizer.initialize();
      dataflowOptimizer.optimize();
      programIr.setMethods(dataflowOptimizer.getOptimizedMethods());
      nLinesOfCodeReductionFactor = (oldNLinesOfCode - countLinesOfCode()) / oldNLinesOfCode;

    }
    compilationState = CompilationState.DATAFLOW_OPTIMIZED;

    System.out.println(programIr.mergeProgram());
  }

  private void generateAssembly() {
    assert compilationState == CompilationState.DATAFLOW_OPTIMIZED;

    programIr.getMethods()
             .forEach(method -> SSA.deconstruct(
                 method,
                 programIr
             ));
    programIr.renumberLabels();

    if (compilationContext.debugModeOn()) {
      System.out.println("After optimization");
      System.out.println(programIr.mergeProgram());
      System.out.format(
          "lines of code reduced by a factor of: %f\n",
          nLinesOfCodeReductionFactor
      );
    }

    var registerAllocator = new RegisterAllocator(programIr);
    programIr.findGlobals();
    var x64AsmWriter = new X86AsmWriter(
        programIr,
        registerAllocator,
        compilationContext
    );
    var x86Program = x64AsmWriter.getX86Program();
    outputStream.println(x86Program);
    if (compilationContext.debugModeOn()) System.out.println(x86Program);
    compilationState = CompilationState.ASSEMBLED;
  }

  enum CompilationState {
    INITIALIZED, SCANNED, PARSED, SEM_CHECKED, CFG_GENERATED, SSA_GENERATED, IR_GENERATED, DATAFLOW_OPTIMIZED, ASSEMBLED, COMPLETED
  }
}
