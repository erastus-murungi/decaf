package decaf;

import decaf.common.TestRunner;

class Main {
  public static void main(String[] args) {
    var passed = TestRunner.testScanner(false);
    System.out.println(passed);
  }
}
