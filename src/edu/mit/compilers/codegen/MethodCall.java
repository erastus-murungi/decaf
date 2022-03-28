package edu.mit.compilers.codegen;

import edu.mit.compilers.ast.AST;

public class MethodCall extends ThreeAddressCode {
    String label;

    public MethodCall(AST source) {
        super(source);
    }
}
