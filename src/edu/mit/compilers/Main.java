package edu.mit.compilers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import edu.mit.compilers.utils.CLI;
import edu.mit.compilers.utils.Compilation;
import edu.mit.compilers.utils.TestRunner;

class Main {
    public static void main(String[] args) throws IOException {
        final String[] optnames = {"all", "cp", "cse", "dce", "regalloc"};
        CLI.parse(args, optnames);
        CLI.outfile = "test.s";
//        String sourceCode = "import printf; \n void main() {int x; x = (1 + 6 * 3); printf(\"%d\", x);}";
//        new Compilation(sourceCode).run();
//          new Compilation(new FileInputStream("tests/codegen/input/11-big-array.dcf"), true).run();
//        new Compilation().run();
//        TestRunner.run();
//        TestRunner.testCodegen();
//        CLI.debug = true;
//        for (int i = 0 ; i < 10; i++) {
//            var res = TestRunner.testCodegenSingleFile("11-big-array.dcf");
//            if (!res)
//                break;
//        }
//        CLI.debug = true;
        var res = TestRunner.testCodegenSingleFile("16-qsort.dcf");

    }
}
