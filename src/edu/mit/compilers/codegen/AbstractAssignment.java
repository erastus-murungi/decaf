package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;

public abstract class AbstractAssignment extends ThreeAddressCode {
    public final String dst;
    public AbstractAssignment(String dst, AST source) {
        super(source);
        this.dst = dst;
    }

    public AbstractAssignment(String dst, AST source, String comment) {
        super(source, comment);
        this.dst = dst;
    }
}
