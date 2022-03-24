package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;

public abstract class ThreeAddressCode {
    AST source;

    public ThreeAddressCode(AST source) {
        this.source = source;
    }
}
