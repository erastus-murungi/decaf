package decaf.common;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import decaf.asm.X86AsmWriter;
import decaf.ast.AST;
import decaf.cfg.ControlFlowGraph;
import decaf.codegen.BasicBlockToInstructionListConverter;
import decaf.dataflow.DataflowOptimizer;
import decaf.dataflow.passes.InstructionSimplifyIrPass;
import decaf.grammar.Parser;
import decaf.grammar.Scanner;
import decaf.ir.SemanticCheckingManager;
import decaf.regalloc.RegisterAllocator;
import decaf.ssa.SSA;

public class Compilation {
  private final static String osName = System.getProperty("os.name")
                                             .replaceAll(
                                                 "\\s",
                                                 ""
                                             )
                                             .toLowerCase(Locale.ROOT);
  private final int nLinesRemovedByAssemblyOptimizer = 0;
  String output = null;
  private String sourceCode;
  private Scanner scanner;
  private Parser parser;
  private SemanticCheckingManager semanticChecker;
  private ControlFlowGraph cfg;
  private BasicBlockToInstructionListConverter basicBlockToInstructionListConverter;
  private ProgramIr programIr;
  private DecafExceptionProcessor decafExceptionProcessor;
  private PrintStream outputStream;
  private CompilationState compilationState;
  private double nLinesOfCodeReductionFactor = 0.0D;
  private static Logger logger = Logger.getLogger(
      Compilation.class.getName()
  );

  public Compilation(
      String filenameOrSourceCode,
      boolean debug,
      boolean isFilename
  ) throws FileNotFoundException {
    if (isFilename) {
      CompilationContext.setAsmOutputFilename(filenameOrSourceCode);
      specificTestFileInitialize(new FileInputStream(filenameOrSourceCode));
    } else {
      this.sourceCode = filenameOrSourceCode;
    }
    initialize();
    CompilationContext.setDebugModeOn(debug);
  }

  public Compilation() throws FileNotFoundException {
    defaultInitialize();
    initialize();
  }

  public Compilation(
      InputStream inputStream,
      boolean debug
  ) throws FileNotFoundException {
    specificTestFileInitialize(inputStream);
    initialize();
    CompilationContext.setDebugModeOn(debug);
  }

  public Compilation(
      String filename,
      boolean debug
  ) throws FileNotFoundException {
    this(
        filename,
        debug,
        false
    );
  }

  public Compilation(InputStream inputStream) throws FileNotFoundException {
    specificTestFileInitialize(inputStream);
    initialize();
  }

  public int getNLinesRemovedByAssemblyOptimizer() {
    return nLinesRemovedByAssemblyOptimizer;
  }

  public double getNLinesOfCodeReductionFactor() {
    return nLinesOfCodeReductionFactor;
  }

  public AST getAstRoot() {
    return parser.getRoot();
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
        if (CompilationContext.isDebugModeOn()) {
          System.out.println(Utils.getStringFromInputStream(process.getErrorStream()));
          System.out.println(Utils.getStringFromInputStream(process.getInputStream()));
        }
      } catch (IOException e) {
        e.printStackTrace();
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
            Utils.getStringFromInputStream(process.getErrorStream()) +
                Utils.getStringFromInputStream(process.getInputStream());
        else output = "TIMEOUT";
        if (CompilationContext.isDebugModeOn()) System.out.println(output);
      } catch (IOException e) {
        e.printStackTrace();
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

  private void specificTestFileInitialize(InputStream inputStream) {
    CompilationContext.setDebugModeOn(true);
    sourceCode = Utils.getStringFromInputStream(inputStream);
  }

  private void defaultInitialize() throws FileNotFoundException {
    InputStream inputStream = CompilationContext.getSourceFilename() ==
        null ? System.in: new FileInputStream(CompilationContext.getSourceFilename());
    sourceCode = Utils.getStringFromInputStream(inputStream);
  }

  private void initialize() throws FileNotFoundException {
    outputStream = CompilationContext.getAsmOutputFilename() ==
        null ? System.out: new java.io.PrintStream(new FileOutputStream(CompilationContext.getAsmOutputFilename()));
    decafExceptionProcessor = new DecafExceptionProcessor(sourceCode);
    compilationState = CompilationState.INITIALIZED;
  }

  private void runScanner() {
    assert compilationState == CompilationState.INITIALIZED;
    scanner = new Scanner(
        sourceCode,
        decafExceptionProcessor
    );
    scanner.setTrace(CompilationContext.isDebugModeOn());
    compilationState = CompilationState.SCANNED;
  }

  private void runParser() {
    assert compilationState == CompilationState.SCANNED;
    parser = new Parser(scanner, decafExceptionProcessor, logger);
    parser.setTrace(CompilationContext.isDebugModeOn());
    parser.program();

    if (parser.hasError()) {
      parser.errors.forEach(Throwable::printStackTrace);
      System.exit(1);
    }
    compilationState = CompilationState.PARSED;
    if (CompilationContext.isDebugModeOn()) System.out.println(parser.getRoot()
                                                                     .getSourceCode());
  }

  private void runSemanticsChecker() {
    assert compilationState == CompilationState.PARSED;
    semanticChecker = new SemanticCheckingManager(parser.getRoot());
    semanticChecker.setTrace(CompilationContext.isDebugModeOn());
    semanticChecker.runChecks(decafExceptionProcessor);
    if (semanticChecker.hasError()) {
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

      if (CompilationContext.isDebugModeOn()) {
        System.out.println("before InstructionSimplifyPass");
        System.out.println(parser.getRoot()
                                 .getSourceCode());
      }

      InstructionSimplifyIrPass.run(parser.getRoot());
      if (CompilationContext.isDebugModeOn()) {
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
    if (CompilationContext.isDebugModeOn()) {
      generateSymbolTablePdfs();
    }
    if (CompilationContext.isDebugModeOn()) {
      generateCFGVisualizationPdfs();
    }
    compilationState = CompilationState.IR_GENERATED;
    if (CompilationContext.isDebugModeOn()) {
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
      if (CompilationContext.isDebugModeOn()) {
        System.out.println("Before optimization");
        System.out.println(programIr.mergeProgram());
      }
      programIr.setGlobals(basicBlockToInstructionListConverter.getGlobalNames());
      var dataflowOptimizer = new DataflowOptimizer(programIr);
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

    if (CompilationContext.isDebugModeOn()) {
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
        registerAllocator
    );
    var x86Program = x64AsmWriter.getX86Program();
    outputStream.println(x86Program);
    if (CompilationContext.isDebugModeOn()) System.out.println(x86Program);
    compilationState = CompilationState.ASSEMBLED;
  }

  enum CompilationState {
    INITIALIZED, SCANNED, PARSED, SEM_CHECKED, CFG_GENERATED, SSA_GENERATED, IR_GENERATED, DATAFLOW_OPTIMIZED, ASSEMBLED, COMPLETED
  }
}
