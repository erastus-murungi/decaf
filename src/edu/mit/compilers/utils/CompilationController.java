package edu.mit.compilers.utils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;

import edu.mit.compilers.asm.X64CodeConverter;
import edu.mit.compilers.asm.X64Program;
import edu.mit.compilers.ast.AST;
import edu.mit.compilers.cfg.CFGGenerator;
import edu.mit.compilers.cfg.iCFGVisitor;
import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.ThreeAddressCodesListConverter;
import edu.mit.compilers.codegen.codes.MethodBegin;
import edu.mit.compilers.dataflow.DataflowOptimizer;
import edu.mit.compilers.dataflow.passes.InstructionSimplifyPass;
import edu.mit.compilers.grammar.DecafParser;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.Token;
import edu.mit.compilers.ir.DecafSemanticChecker;
import edu.mit.compilers.tools.CLI;

public class CompilationController {
    private String sourceCode;
    private DecafScanner scanner;
    private DecafParser parser;
    private DecafSemanticChecker semanticChecker;
    private CFGGenerator cfgGenerator;
    private ThreeAddressCodesListConverter threeAddressCodesListConverter;
    private iCFGVisitor iCFGVisitor;

    private Pair<ThreeAddressCodeList, List<MethodBegin>> programIr;

    private DecafExceptionProcessor decafExceptionProcessor;
    private PrintStream outputStream;
    private CompilationState compilationState;

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

    public CompilationController(String sourceCode) throws FileNotFoundException {
        this.sourceCode = sourceCode;
        initialize();
        CLI.target = CLI.Action.ASSEMBLY;

    }

    public CompilationController() throws FileNotFoundException {
        defaultInitialize();
        initialize();
    }

    private void defaultInitialize() throws FileNotFoundException {
//        CLI.infile = "tests/codegen/input/06-control-flow.dcf";
//        CLI.opts = new boolean[1];
//        CLI.target = CLI.Action.ASSEMBLY;
        InputStream inputStream = CLI.infile == null ? System.in : new java.io.FileInputStream(CLI.infile);
        sourceCode = Utils.getStringFromInputStream(inputStream);
    }

    private void initialize() throws FileNotFoundException {
        outputStream = CLI.outfile == null ? System.out : new java.io.PrintStream(new java.io.FileOutputStream(CLI.outfile));
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
        semanticChecker = new DecafSemanticChecker(parser.getRoot(), decafExceptionProcessor);
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
        GraphVizPrinter.printGraph(copy);
    }

    private boolean shouldOptimize() {
//        return true;
        return CLI.opts != null && CLI.opts.length != 0;
    }

    private void generateSymbolTablePdfs() {
        GraphVizPrinter.printSymbolTables(parser.getRoot(), threeAddressCodesListConverter.cfgSymbolTables);
    }

    private void generateCFGs() {
        assert compilationState == CompilationState.SEM_CHECKED;
        if (shouldOptimize()) {
            InstructionSimplifyPass.run(parser.getRoot());
            if (CLI.debug) {
                System.out.println("after InstructionSimplifyPass");
                System.out.println(parser.getRoot());
            }
        }

        cfgGenerator = new CFGGenerator(parser.getRoot(), semanticChecker.globalDescriptor);
        iCFGVisitor = cfgGenerator.buildiCFG();
        if (CLI.debug) {
            generateCFGVisualizationPdfs();
        }
        compilationState = CompilationState.CFG_GENERATED;
    }

    private ThreeAddressCodeList mergeProgram() {
        var programHeader = programIr.first;
        ThreeAddressCodeList tacList = programHeader.clone();
        for (MethodBegin methodBegin : programIr.second()) {
            tacList.add(methodBegin.entryBlock.threeAddressCodeList.flatten());
        }
        return tacList;
    }

    private void generateIr() {
        assert compilationState == CompilationState.CFG_GENERATED;
        threeAddressCodesListConverter = new ThreeAddressCodesListConverter(cfgGenerator);
        programIr = threeAddressCodesListConverter.fill(iCFGVisitor, parser.getRoot());
        if (CLI.debug) {
            generateSymbolTablePdfs();
        }
        compilationState = CompilationState.IR_GENERATED;
    }

    private void runDataflowOptimizationPasses() {
        assert compilationState == CompilationState.IR_GENERATED;
        if (shouldOptimize()) {
            if (CLI.debug) {
                System.out.println("Before optimization");
                System.out.println(mergeProgram());
            }
            var dataflowOptimizer = new DataflowOptimizer(programIr.second, threeAddressCodesListConverter.globalNames);
            dataflowOptimizer.initialize();
            dataflowOptimizer.optimize();
            if (CLI.debug) {
                System.out.println("After optimization");
                System.out.println(mergeProgram());
            }

        }
        compilationState = CompilationState.DATAFLOW_OPTIMIZED;
    }

    private void generateAssembly() {
        assert compilationState == CompilationState.DATAFLOW_OPTIMIZED;
        X64CodeConverter x64CodeConverter = new X64CodeConverter();
        X64Program x64program = x64CodeConverter.convert(mergeProgram());
        outputStream.println(x64program);
        compilationState = CompilationState.ASSEMBLED;
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


