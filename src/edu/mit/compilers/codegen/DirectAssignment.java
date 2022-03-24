package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;

public class DirectAssignment extends ThreeAddressCode{
    String src;
    String dst;

    public DirectAssignment(String src, String dst, AST source) {
        super(source);
        this.src = src;
        this.dst = dst;
    }

    @Override
    public String toString() {
        return String.format("    %s = %s", dst, src);
    }
}
