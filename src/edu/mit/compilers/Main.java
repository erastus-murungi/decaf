package edu.mit.compilers;

import java.io.FileNotFoundException;

import edu.mit.compilers.tools.CLI;
import edu.mit.compilers.utils.CompilationController;

class Main {
    public static void main(String[] args) throws FileNotFoundException {
        CLI.parse(args, new String[0]);
        new CompilationController().run();
    }
}
