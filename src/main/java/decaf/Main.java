package decaf;

import java.io.FileNotFoundException;

import decaf.shared.Compilation;
import decaf.shared.TestRunner;

class Main {
  public static void main(String[] args) throws FileNotFoundException {
    TestRunner.testAll();
  }
}
