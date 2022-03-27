package edu.mit.compilers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.cfg.*;
import edu.mit.compilers.grammar.DecafParser;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.Token;
import edu.mit.compilers.ir.DecafSemanticChecker;
import edu.mit.compilers.tools.CLI;
import edu.mit.compilers.utils.DecafExceptionProcessor;
import edu.mit.compilers.utils.GraphVizPrinter;
import edu.mit.compilers.utils.Utils;

class Main {
    public static void main(String[] args) {
        CLI.parse(args, new String[0]);
        CLI.debug = true;
//        try {
//            CLI.parse(args, new String[0]);
//            InputStream inputStream = CLI.infile == null ? System.in : new java.io.FileInputStream(CLI.infile);
//            PrintStream outputStream = CLI.outfile == null ? System.out : new java.io.PrintStream(new java.io.FileOutputStream(CLI.outfile));
//            String sourceCode = Utils.getStringFromInputStream(inputStream);
//            DecafExceptionProcessor decafExceptionProcessor = new DecafExceptionProcessor(sourceCode);
//            if (CLI.target == CLI.Action.SCAN) {
//                DecafScanner scanner = new DecafScanner(sourceCode, decafExceptionProcessor);
//                scanner.setTrace(CLI.debug);
//                Token token;
//                boolean done = false;
//                boolean error = false;
//                while (!done) {
//                    try {
//                        for (token = scanner.nextToken(); token.isNotEOF(); token = scanner.nextToken()) {
//                            String text;
//                            switch (token.tokenType()) {
//                                case ID: {
//                                    text = "IDENTIFIER" + " " + token.lexeme();
//                                    break;
//                                }
//                                case STRING_LITERAL: {
//                                    text = "STRINGLITERAL" + " " + token.lexeme();
//                                    break;
//                                }
//                                case CHAR_LITERAL: {
//                                    text = "CHARLITERAL" + " " + token.lexeme();
//                                    break;
//                                }
//                                case HEX_LITERAL:
//                                case DECIMAL_LITERAL: {
//                                    text = "INTLITERAL" + " " + token.lexeme();
//                                    break;
//                                }
//                                case RESERVED_FALSE:
//                                case RESERVED_TRUE: {
//                                    text = "BOOLEANLITERAL" + " " + token.lexeme();
//                                    break;
//                                }
//                                default: {
//                                    text = token.lexeme();
//                                    break;
//                                }
//                            }
//                            outputStream.println(token.tokenPosition().line() + 1 + " " + text);
//                        }
//                        done = true;
//                    } catch (Exception e) {
//                        System.err.println(CLI.infile + " " + e);
//                        error = true;
//                    }
//                    if (error) {
//                        System.exit(1);
//                    }
//                }
//            } else if (CLI.target == CLI.Action.PARSE || CLI.target == CLI.Action.DEFAULT) {
//                DecafScanner scanner = new DecafScanner(sourceCode, decafExceptionProcessor);
//                DecafParser parser = new DecafParser(scanner);
//                parser.setTrace(CLI.debug);
//                parser.program();
//                if (parser.hasError()) {
//                    System.exit(1);
//                }
//            } else if (CLI.target == CLI.Action.INTER) {
//                DecafScanner scanner = new DecafScanner(sourceCode, decafExceptionProcessor);
//                DecafParser parser = new DecafParser(scanner);
//                //          parser.setTrace(CLI.debug);
//                parser.program();
//                AST programNode = parser.getRoot();
//
//                DecafSemanticChecker semChecker = new DecafSemanticChecker(programNode);
//                semChecker.setTrace(CLI.debug);
//                semChecker.runChecks(decafExceptionProcessor);
//                if (semChecker.hasError()) {
//                    System.exit(1);
//                }
//            }
//        } catch (Exception e) {
//            System.err.println(CLI.infile + " " + e);
//        }
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream("tests/codegen/input/x-08-loop-break-continue.dcf");
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        assert fileInputStream != null;
        String sourceCode = Utils.getStringFromInputStream(fileInputStream);
        DecafExceptionProcessor decafExceptionProcessor = new DecafExceptionProcessor(sourceCode);
        DecafScanner scanner = new DecafScanner(sourceCode, decafExceptionProcessor);
        DecafParser parser = new DecafParser(scanner);
        parser.setTrace(CLI.debug);
        if (parser.hasError()) {
            System.out.println(parser);
            System.exit(1);
        }
        parser.program();
        AST programNode = parser.getRoot();

        DecafSemanticChecker semChecker = new DecafSemanticChecker(programNode);
        semChecker.setTrace(CLI.debug);
        semChecker.runChecks(decafExceptionProcessor);
        if (semChecker.hasError()) {
            semChecker.printAllExceptions(decafExceptionProcessor);
            System.exit(1);
        }
        CFGGenerator cfgGenerator = new CFGGenerator(programNode, semChecker.globalDescriptor);
        iCFGVisitor visitor = cfgGenerator.buildiCFG();
        if (CLI.debug) {
            HashMap<String, CFGBlock> copy = (HashMap<String, CFGBlock>) visitor.methodCFGBlocks.clone();
            copy.put("global", visitor.initialGlobalBlock);
            GraphVizPrinter.printGraph(copy);
            System.out.println(programNode.getSourceCode());
        }
    }
}