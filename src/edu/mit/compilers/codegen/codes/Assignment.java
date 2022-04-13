package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.names.AssignableName;

public abstract class Assignment extends ThreeAddressCode {
    public final AssignableName dst;

    public Assignment(AssignableName dst, AST source) {
        super(source);
        this.dst = dst;
    }

    public Assignment(AssignableName dst, AST source, String comment) {
        super(source, comment);
        this.dst = dst;
    }

    public AssignableName getResultLocation() {
        return dst;
    }
}
