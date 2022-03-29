package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;

public class PushParameter extends ThreeAddressCode {
    public String which;

    public PushParameter(String which, AST source) {
        super(source);
        this.which = which;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s", DOUBLE_INDENT, "PushParameter", which);
    }
}
