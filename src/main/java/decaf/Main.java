package decaf;

import java.io.IOException;

import decaf.common.CompilationContext;
import decaf.common.TestRunner;

class Main {
  public static void main(String[] args) throws IOException {
    TestRunner.testParser("legal-04", true);
//    TestRunner.testScanner();
  }
}
