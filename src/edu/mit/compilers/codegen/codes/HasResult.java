package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import java.util.Set;

public abstract class HasResult extends ThreeAddressCode implements Cloneable {
    public AssignableName dst;

    public HasResult(AssignableName dst, AST source) {
        super(source);
        this.dst = dst;
    }

    public HasResult(AssignableName dst, AST source, String comment) {
        super(source, comment);
        this.dst = dst;
    }

    public AssignableName getResultLocation() {
        return dst;
    }

    public void setResultLocation(AssignableName dst) {
        this.dst = dst;
    }

    public abstract Set<AbstractName> getComputationVariables();

    @Override
    public HasResult clone() {
        try {
            HasResult clone = (HasResult) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            clone.dst = dst;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
