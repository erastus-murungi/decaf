package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;

public abstract class Assignment extends ThreeAddressCode {
    public Assignment(AST source) {
        super(source);
    }
}
