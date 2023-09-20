package decaf;

import decaf.shared.TestRunner;

import java.io.FileNotFoundException;

class Main {
  public static void main(String[] args) throws FileNotFoundException {
    TestRunner.testCfgBuilding("if-true-branch-nest1", true, true);
  }
}
