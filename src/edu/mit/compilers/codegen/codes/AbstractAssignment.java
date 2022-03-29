package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.names.AssignableName;

public abstract class AbstractAssignment extends ThreeAddressCode {
    public final AssignableName dst;
    public AbstractAssignment(AssignableName dst, AST source) {
        super(source);
        this.dst = dst;
    }

    public AbstractAssignment(AssignableName dst, AST source, String comment) {
        super(source, comment);
        this.dst = dst;
    }
}
