package edu.mit.compilers;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.grammar.DecafParser;
import edu.mit.compilers.grammar.DecafScanner;
import edu.mit.compilers.grammar.Token;
import edu.mit.compilers.ir.DecafSemanticChecker;
import edu.mit.compilers.tools.CLI;
import java.io.*;

class Main {
  public static void main(String[] args) {
    try {
      CLI.parse(args, new String[0]);
      InputStream inputStream = CLI.infile == null
                                    ? System.in
                                    : new java.io.FileInputStream(CLI.infile);
      PrintStream outputStream =
          CLI.outfile == null ? System.out
                              : new java.io.PrintStream(
                                    new java.io.FileOutputStream(CLI.outfile));
      if (CLI.target == CLI.Action.SCAN) {
        DecafScanner scanner =
            new DecafScanner(new DataInputStream(inputStream));
        scanner.setTrace(CLI.debug);
        Token token;
        boolean done = false;
        boolean error = false;
        while (!done) {
          try {
            for (token = scanner.nextToken(); token.isNotEOF();
                 token = scanner.nextToken()) {
              String text = token.lexeme();
              text = switch (token.tokenType()) {
                  case ID -> "IDENTIFIER" + " " + token.lexeme();
                  case STRING_LITERAL -> "STRINGLITERAL" + " " + token.lexeme();
                  case CHAR_LITERAL -> "CHARLITERAL" + " " + token.lexeme();
                  case HEX_LITERAL, DECIMAL_LITERAL -> "INTLITERAL" + " " + token.lexeme();
                  case RESERVED_FALSE, RESERVED_TRUE -> "BOOLEANLITERAL" + " " + token.lexeme();
                  default -> text;
              };
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
          DecafScanner scanner = new DecafScanner(new DataInputStream(inputStream));
          DecafParser parser = new DecafParser(scanner);
          parser.setTrace(CLI.debug);
          parser.program();
          if (parser.hasError()) {
              System.exit(1);
          }
      } else if (CLI.target == CLI.Action.INTER) {
          DecafScanner scanner = new DecafScanner(new DataInputStream(inputStream));
          DecafParser parser = new DecafParser(scanner);
          parser.setTrace(CLI.debug);
          parser.program();
          AST programNode = parser.getRoot();

          DecafSemanticChecker semChecker = new DecafSemanticChecker(programNode);

          if (semChecker.hasError()) {
              System.exit(1);
          }
      }
    } catch (Exception e) {
        System.err.println(CLI.infile + " " + e);
    }
  }
}