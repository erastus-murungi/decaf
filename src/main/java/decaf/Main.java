package decaf;

import decaf.shared.Compilation;
import decaf.shared.TestRunner;

import java.io.FileNotFoundException;

class Main {
  public static void main(String[] args) throws FileNotFoundException {
    var compilation = Compilation.forTestFile("/Users/erastusmurungi/Code/decaf/testdata/cfg/input/simple-if.dcf", true);
    compilation.run();
    TestRunner.testAll();
  }
}
