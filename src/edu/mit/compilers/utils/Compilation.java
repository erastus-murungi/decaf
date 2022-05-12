package edu.mit.compilers.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;

import edu.mit.compilers.asm.X64CodeConverter;
import edu.mit.compilers.asm.X64Program;
import edu.mit.compilers.ast.AST;
import edu.mit.compilers.cfg.CFGGenerator;
import edu.mit.compilers.cfg.iCFGVisitor;
import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.InstructionListConverter;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.dataflow.DataflowOptimizer;
import edu.mit.compilers.dataflow.passes.InstructionSimplifyIrPass;
import edu.mit.compilers.grammar.DecafParser;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.Token;
import edu.mit.compilers.ir.DecafSemanticChecker;
import edu.mit.compilers.registerallocation.RegisterAllocation;
import edu.mit.compilers.tools.CLI;

public class Compilation {
    private String sourceCode;
    private DecafScanner scanner;
    private DecafParser parser;
    private DecafSemanticChecker semanticChecker;
    private CFGGenerator cfgGenerator;
    private InstructionListConverter instructionListConverter;
    private iCFGVisitor iCFGVisitor;

    private ProgramIr programIr;

    private DecafExceptionProcessor decafExceptionProcessor;
    private PrintStream outputStream;
    private CompilationState compilationState;

    private double nLinesOfCodeReductionFactor = 0.0D;
    private int nLinesRemovedByAssemblyOptimizer = 0;

    public int getNLinesRemovedByAssemblyOptimizer() {
        return nLinesRemovedByAssemblyOptimizer;
    }

    public double getNLinesOfCodeReductionFactor() {
        return nLinesOfCodeReductionFactor;
    }

    public AST getAstRoot() {
        return parser.getRoot();
    }

    enum CompilationState {
        INITIALIZED,
        SCANNED,
        PARSED,
        SEM_CHECKED,
        CFG_GENERATED,
        IR_GENERATED,
        DATAFLOW_OPTIMIZED,
        ASSEMBLED,
        COMPLETED
    }

    private void runNextStep() {
        switch (compilationState) {
            case SCANNED: {
                runParser();
                break;
            }
            case PARSED: {
                runSemanticsChecker();
                break;
            }
            case SEM_CHECKED: {
                generateCFGs();
                break;
            }
            case CFG_GENERATED: {
                generateIr();
                break;
            }
            case IR_GENERATED: {
                runDataflowOptimizationPasses();
                break;
            }
            case DATAFLOW_OPTIMIZED: {
                generateAssembly();
                break;
            }
        }
    }

    public void run() throws FileNotFoundException {
        runScanner();
        while (compilationState != CompilationState.COMPLETED && compilationState != CompilationState.ASSEMBLED) {
            runNextStep();
        }
    }

    public Compilation(String sourceCode, boolean debug) throws FileNotFoundException {
        this.sourceCode = sourceCode;
        initialize();
        CLI.debug = debug;
        CLI.target = CLI.Action.ASSEMBLY;
        CLI.opts = new boolean[]{true};

    }

    public Compilation() throws FileNotFoundException {
        defaultInitialize();
        initialize();
    }

    private void specificTestFileInitialize(InputStream inputStream) {
        CLI.opts = new boolean[]{true};
        CLI.target = CLI.Action.ASSEMBLY;
        CLI.debug = true;
        sourceCode = Utils.getStringFromInputStream(inputStream);
    }

    public Compilation(InputStream inputStream, boolean debug) throws FileNotFoundException {
        specificTestFileInitialize(inputStream);
        initialize();
        CLI.debug = debug;
    }

    public Compilation(InputStream inputStream) throws FileNotFoundException {
        specificTestFileInitialize(inputStream);
        initialize();
    }

    private void defaultInitialize() throws FileNotFoundException {
        InputStream inputStream = CLI.infile == null ? System.in : new FileInputStream(CLI.infile);
        sourceCode = Utils.getStringFromInputStream(inputStream);
    }

    private void initialize() throws FileNotFoundException {
        outputStream = CLI.outfile == null ? System.out : new java.io.PrintStream(new FileOutputStream(CLI.outfile));
        decafExceptionProcessor = new DecafExceptionProcessor(sourceCode);
        compilationState = CompilationState.INITIALIZED;
    }

    private void runScanner() {
        assert compilationState == CompilationState.INITIALIZED;
        scanner = new DecafScanner(sourceCode, decafExceptionProcessor);
        scanner.setTrace(CLI.debug);
        if (CLI.target == CLI.Action.SCAN || CLI.target == CLI.Action.DEFAULT) {
            patternMatchTest();
            compilationState = CompilationState.COMPLETED;
        } else {
            compilationState = CompilationState.SCANNED;
        }
    }

    private void runParser() {
        assert compilationState == CompilationState.SCANNED;
        parser = new DecafParser(scanner);
        parser.setTrace(CLI.debug);
        parser.program();

        if (parser.hasError()) {
            parser.errors.forEach(Throwable::printStackTrace);
            System.exit(1);
        }
        if (CLI.target == CLI.Action.PARSE) {
            compilationState = CompilationState.COMPLETED;
        } else {
            compilationState = CompilationState.PARSED;
        }
    }

    private void runSemanticsChecker() {
        assert compilationState == CompilationState.PARSED;
        semanticChecker = new DecafSemanticChecker(parser.getRoot());
        semanticChecker.setTrace(CLI.debug);
        semanticChecker.runChecks(decafExceptionProcessor);
        if (semanticChecker.hasError()) {
            System.exit(1);
        }
        if (CLI.target == CLI.Action.INTER) {
            compilationState = CompilationState.COMPLETED;
        } else {
            compilationState = CompilationState.SEM_CHECKED;
        }
    }

    private void generateCFGVisualizationPdfs() {
        var copy = new HashMap<>(iCFGVisitor.methodCFGBlocks);
        copy.put("globals", iCFGVisitor.initialGlobalBlock);
//        GraphVizPrinter.printGraph(copy, (basicBlock -> basicBlock.threeAddressCodeList.getCodes().stream().map(ThreeAddressCode::repr).collect(Collectors.joining("\n"))));
        GraphVizPrinter.printGraph(copy);
    }

    private boolean shouldOptimize() {
        for (boolean opt : CLI.opts)
            if (opt)
                return true;
        return false;
    }

    private void generateSymbolTablePdfs() {
        GraphVizPrinter.printSymbolTables(parser.getRoot(), instructionListConverter.cfgSymbolTables);
    }

    private void generateCFGs() {
        assert compilationState == CompilationState.SEM_CHECKED;
        if (shouldOptimize()) {
            if (CLI.debug) {
                System.out.println("before InstructionSimplifyPass");
                System.out.println(parser.getRoot()
                        .getSourceCode());
            }
            InstructionSimplifyIrPass.run(parser.getRoot());
            if (CLI.debug) {
                System.out.println("after InstructionSimplifyPass");
                System.out.println(parser.getRoot()
                        .getSourceCode());
            }
        }

        cfgGenerator = new CFGGenerator(parser.getRoot(), semanticChecker.globalDescriptor);
        iCFGVisitor = cfgGenerator.buildiCFG();
        compilationState = CompilationState.CFG_GENERATED;
    }

    private InstructionList mergeProgram() {
        var programHeader = programIr.headerInstructions.copy();
        var tacList = programHeader.copy();
        for (MethodBegin methodBegin : programIr.methodBeginList) {
            tacList.addAll(methodBegin.entryBlock.instructionList.flatten());
        }
        return tacList;
    }

    private void generateIr() {
        assert compilationState == CompilationState.CFG_GENERATED;
        instructionListConverter = new InstructionListConverter(cfgGenerator);
        programIr = instructionListConverter.fill(iCFGVisitor, parser.getRoot());
        if (CLI.debug) {
            generateSymbolTablePdfs();
        }
        if (CLI.debug) {
            generateCFGVisualizationPdfs();
        }
        compilationState = CompilationState.IR_GENERATED;
    }

    private int countLinesOfCode() {
        return mergeProgram().size();
    }

    private void runDataflowOptimizationPasses() {
        assert compilationState == CompilationState.IR_GENERATED;
        double oldNLinesOfCode;
        oldNLinesOfCode = countLinesOfCode();
        if (shouldOptimize()) {
            if (CLI.debug) {
                System.out.println("Before optimization");
                System.out.println(mergeProgram());
            }
            var dataflowOptimizer = new DataflowOptimizer(programIr.methodBeginList, instructionListConverter.globalNames);
            dataflowOptimizer.initialize();
            dataflowOptimizer.optimize();
            programIr.methodBeginList = dataflowOptimizer.allMethods;
            nLinesOfCodeReductionFactor = (oldNLinesOfCode - countLinesOfCode()) / oldNLinesOfCode;
            if (CLI.debug) {
                System.out.println("After optimization");
                System.out.println(mergeProgram());
                System.out.format("lines of code reduced by a factor of: %f\n", nLinesOfCodeReductionFactor);
            }

        }
        compilationState = CompilationState.DATAFLOW_OPTIMIZED;
    }

    private void generateAssembly() {
        assert compilationState == CompilationState.DATAFLOW_OPTIMIZED;

        X64CodeConverter x64CodeConverter;
        if (true) {
            var registerAllocation = new RegisterAllocation(programIr);
            x64CodeConverter = new X64CodeConverter(registerAllocation.getVariableToRegisterMap(), registerAllocation.getMethodToLiveRegistersInfo());
        } else {
            x64CodeConverter = new X64CodeConverter();
        }
        X64Program x64program = x64CodeConverter.convert(mergeProgram());
        if (shouldOptimize()) {
//            var peepHoleOptimizationAsmPass = new PeepHoleOptimizationAsmPass(x64program);
//            peepHoleOptimizationAsmPass.run();
//            nLinesRemovedByAssemblyOptimizer = peepHoleOptimizationAsmPass.getNumInstructionsRemoved();
        }
        outputStream.println(x64program);
        compilationState = CompilationState.ASSEMBLED;
    }

    public InstructionList getInstructionList() {
        if (compilationState != CompilationState.IR_GENERATED && compilationState != CompilationState.ASSEMBLED) {
            throw new IllegalStateException("instruction list has not been created yet");
        }
        return mergeProgram();
    }

    private void patternMatchTest() {
        Token token;
        boolean done = false;
        boolean error = false;
        while (!done) {
            try {
                for (token = scanner.nextToken(); token.isNotEOF(); token = scanner.nextToken()) {
                    String text;
                    switch (token.tokenType()) {
                        case ID: {
                            text = "IDENTIFIER" + " " + token.lexeme();
                            break;
                        }
                        case STRING_LITERAL: {
                            text = "STRINGLITERAL" + " " + token.lexeme();
                            break;
                        }
                        case CHAR_LITERAL: {
                            text = "CHARLITERAL" + " " + token.lexeme();
                            break;
                        }
                        case HEX_LITERAL:
                        case DECIMAL_LITERAL: {
                            text = "INTLITERAL" + " " + token.lexeme();
                            break;
                        }
                        case RESERVED_FALSE:
                        case RESERVED_TRUE: {
                            text = "BOOLEANLITERAL" + " " + token.lexeme();
                            break;
                        }
                        default: {
                            text = token.lexeme();
                            break;
                        }
                    }
                    outputStream.println(token.tokenPosition()
                            .line() + 1 + " " + text);
                }
                done = true;
            } catch (Exception e) {
                System.err.println(CLI.infile + " " + e);
                error = true;
            }
            if (error) {
                System.exit(1);
            }
        }
    }
}
