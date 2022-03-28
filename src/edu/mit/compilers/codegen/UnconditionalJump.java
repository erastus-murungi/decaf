package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;

public class UnconditionalJump extends ThreeAddressCode {
    public UnconditionalJump(AST source) {
        super(source);
    }
}
