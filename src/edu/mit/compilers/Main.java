package edu.mit.compilers;

import java.io.FileNotFoundException;

import edu.mit.compilers.tools.CLI;
import edu.mit.compilers.utils.Compilation;

class Main {
    public static void main(String[] args) throws FileNotFoundException {
        final String[] optnames = {"cse", "all", "cp"};
        CLI.parse(args, optnames);
//        String sourceCode = "import printf; \n void main() {int x; x = (1 + 6 * 3); printf(\"%d\", x);}";
//        new Compilation(sourceCode).run();
        new Compilation().run();
    }
}
