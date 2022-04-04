package edu.mit.compilers;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import edu.mit.compilers.asm.X64CodeConverter;
import edu.mit.compilers.asm.X64Program;
import edu.mit.compilers.ast.AST;
import edu.mit.compilers.ast.Program;
import edu.mit.compilers.cfg.*;
import edu.mit.compilers.codegen.ThreeAddressCodeList;
import edu.mit.compilers.codegen.ThreeAddressCodesListConverter;
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
        try {
            CLI.parse(args, new String[0]);
            InputStream inputStream = CLI.infile == null ? System.in : new java.io.FileInputStream(CLI.infile);
            PrintStream outputStream = CLI.outfile == null ? System.out : new java.io.PrintStream(new java.io.FileOutputStream(CLI.outfile));

            String sourceCode = Utils.getStringFromInputStream(inputStream);
            DecafExceptionProcessor decafExceptionProcessor = new DecafExceptionProcessor(sourceCode);
            if (CLI.target == CLI.Action.SCAN) {
                DecafScanner scanner = new DecafScanner(sourceCode, decafExceptionProcessor);
                scanner.setTrace(CLI.debug);
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
                            outputStream.println(token.tokenPosition().line() + 1 + " " + text);
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
            } else if (CLI.target == CLI.Action.PARSE || CLI.target == CLI.Action.DEFAULT) {
                DecafScanner scanner = new DecafScanner(sourceCode, decafExceptionProcessor);
                DecafParser parser = new DecafParser(scanner);
                parser.setTrace(CLI.debug);
                parser.program();
                if (parser.hasError()) {
                    System.exit(1);
                }
            } else if (CLI.target == CLI.Action.INTER) {
                DecafScanner scanner = new DecafScanner(sourceCode, decafExceptionProcessor);
                DecafParser parser = new DecafParser(scanner);
                //          parser.setTrace(CLI.debug);
                parser.program();
                AST programNode = parser.getRoot();

                DecafSemanticChecker semChecker = new DecafSemanticChecker(programNode);
                semChecker.setTrace(CLI.debug);
                semChecker.runChecks(decafExceptionProcessor);
                if (semChecker.hasError()) {
                    System.exit(1);
                }
            } else if (CLI.target == CLI.Action.ASSEMBLY) {
                DecafScanner scanner = new DecafScanner(sourceCode, decafExceptionProcessor);
                DecafParser parser = new DecafParser(scanner);
                //          parser.setTrace(CLI.debug);
                parser.program();
                Program programNode = parser.getRoot();

                DecafSemanticChecker semChecker = new DecafSemanticChecker(programNode);
                semChecker.setTrace(CLI.debug);
                semChecker.runChecks(decafExceptionProcessor);
                if (semChecker.hasError()) {
                    System.exit(1);
                }

                CFGGenerator cfgGenerator = new CFGGenerator(programNode, semChecker.globalDescriptor);
                iCFGVisitor visitor = cfgGenerator.buildiCFG();
                if (CLI.debug) {
                    HashMap<String, CFGBlock> copy = (HashMap<String, CFGBlock>) visitor.methodCFGBlocks.clone();
                    copy.put("global", visitor.initialGlobalBlock);
                    GraphVizPrinter.printGraph(copy);
                }

                ThreeAddressCodesListConverter threeAddressCodesListConverter = new ThreeAddressCodesListConverter(cfgGenerator.globalDescriptor);
                ThreeAddressCodeList threeAddressCodeList = threeAddressCodesListConverter.fill(visitor, programNode);
                if (CLI.debug) {
                    System.out.println(programNode.getSourceCode());
                    System.out.println(threeAddressCodeList);
                    GraphVizPrinter.printSymbolTables(programNode, threeAddressCodesListConverter.cfgSymbolTables);
                }

                X64CodeConverter x64CodeConverter = new X64CodeConverter();
                X64Program x64Program = x64CodeConverter.convert(threeAddressCodeList);

                outputStream.print(x64Program.toString());

            }
        } catch (Exception e) {
            System.err.println(CLI.infile + " " + e);
        }


//         CLI.parse(args, new String[0]);
//         CLI.debug = true;
//         FileInputStream fileInputStream = null;
//         try {
//             fileInputStream = new FileInputStream(CLI.infile);
//         } catch (FileNotFoundException ex) {
//             ex.printStackTrace();
//         }
//         assert fileInputStream != null;
//         String sourceCode = Utils.getStringFromInputStream(fileInputStream);
//         DecafExceptionProcessor decafExceptionProcessor = new DecafExceptionProcessor(sourceCode);
//         DecafScanner scanner = new DecafScanner(sourceCode, decafExceptionProcessor);
//         DecafParser parser = new DecafParser(scanner);
//        parser.setTrace(CLI.debug);
//        if (parser.hasError()) {
//            System.out.println(parser);
//            System.exit(1);
//        }
//         parser.program();
//         Program programNode = parser.getRoot();
//         DecafSemanticChecker semChecker = new DecafSemanticChecker(programNode);
//         semChecker.setTrace(CLI.debug);
//         semChecker.runChecks(decafExceptionProcessor);
//        if (semChecker.hasError()) {
//            semChecker.printAllExceptions(decafExceptionProcessor);
//            System.exit(1);
//        }
//         CFGGenerator cfgGenerator = new CFGGenerator(programNode, semChecker.globalDescriptor);
//         iCFGVisitor visitor = cfgGenerator.buildiCFG();
// //        if (CLI.debug) {
// //            HashMap<String, CFGBlock> copy = (HashMap<String, CFGBlock>) visitor.methodCFGBlocks.clone();
// //            copy.put("global", visitor.initialGlobalBlock);
// //            GraphVizPrinter.printGraph(copy);
// //        }

//         ThreeAddressCodesListConverter threeAddressCodesListConverter = new ThreeAddressCodesListConverter(cfgGenerator.globalDescriptor);
//         ThreeAddressCodeList threeAddressCodeList = threeAddressCodesListConverter.fill(visitor, programNode);
// //        if (CLI.debug) {
// //            System.out.println(programNode.getSourceCode());
// //            GraphVizPrinter.printSymbolTables(programNode, threeAddressCodesListConverter.cfgSymbolTables);
// //        }

// //        System.out.println(threeAddressCodeList);
//         X64CodeConverter x64CodeConverter = new X64CodeConverter();
//         X64Program x64Program = x64CodeConverter.convert(threeAddressCodeList);
//         try {
//             PrintStream printStream = new PrintStream(new FileOutputStream("tests/output.S"));
//             printStream.println(x64Program);
//         } catch (FileNotFoundException e) {
//             e.printStackTrace();
//         }
//         //System.out.println(x64Program);
    }

}