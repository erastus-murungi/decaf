package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;

public class PopParameter extends ThreeAddressCode {
    String which;

    public PopParameter(String which, AST source) {
        super(source);
        this.which = which;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", "    ", "PopParameter", which);
    }
}
