package decaf;

import java.io.IOException;

import decaf.common.CompilationContext;
import decaf.common.TestRunner;

class Main {
  public static void main(String[] args) throws IOException {
    CompilationContext.setAsmOutputFilename("test.s");
//        String sourceCode = "import printf; \n void main() {int x; x = (1 + 6 * 3); printf(\"%d\", x);}";
//        new Compilation(sourceCode).run();
//          new Compilation(new FileInputStream("tests/misc/simple.dcf"), true).run();
//        new Compilation().run();
//        TestRunner.run();
//        TestRunner.testCodegen();
//        CLI.debug = true;
//        for (int i = 0 ; i < 10; i++) {
//            var res = TestRunner.testCodegenSingleFile("x-33-binop-order.dcf");
//            if (!res)
//                break;
//        }
//    CompilationContext.setDebugModeOn(false);
//    TestRunner.testCodegenSingleFile("01-import.dcf");
    TestRunner.testScanner();

  }
}
