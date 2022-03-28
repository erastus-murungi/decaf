package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;

public abstract class AbstractAssignment extends ThreeAddressCode {
    public AbstractAssignment(AST source) {
        super(source);
    }
}
