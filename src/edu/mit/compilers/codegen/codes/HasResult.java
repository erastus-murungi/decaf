package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.dataflow.operand.Operand;

import java.util.Optional;
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

    public abstract Optional<Operand> getComputationNoArray();

    public Optional<Operand> getComputationNoArrayNoGlobals(Set<AbstractName> globals) {
        Optional<Operand> computationNoArray = getComputationNoArray();
        if (computationNoArray.isPresent()) {
            if (computationNoArray.get().containsAny(globals)) {
                return Optional.empty();
            } else {
                return computationNoArray;
            }
        }
        return computationNoArray;
    }
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
