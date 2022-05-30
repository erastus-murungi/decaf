package edu.mit.compilers;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import edu.mit.compilers.tools.CLI;
import edu.mit.compilers.utils.Compilation;
import edu.mit.compilers.utils.TestRunner;

class Main {
    public static void main(String[] args) throws FileNotFoundException {
        final String[] optnames = {"all", "cp", "cse", "dce", "regalloc"};
        CLI.parse(args, optnames);
//        String sourceCode = "import printf; \n void main() {int x; x = (1 + 6 * 3); printf(\"%d\", x);}";
//        new Compilation(sourceCode).run();
          new Compilation(new FileInputStream("tests/codegen/input/x-02-for-condwithsideeffects.dcf"), true).run();
//        new Compilation().run();
//        TestRunner.run();
    }
}
