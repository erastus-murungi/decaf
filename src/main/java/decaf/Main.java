package decaf;

import java.io.FileNotFoundException;

import decaf.shared.Compilation;
import decaf.shared.TestRunner;

class Main {
  public static void main(String[] args) throws FileNotFoundException {
//    TestRunner.testAll();
    try {
      new Compilation("/Users/erastusmurungi/IdeaProjects/compiler/testdata/codegen/input/02-expr.dcf", true, true).run();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
