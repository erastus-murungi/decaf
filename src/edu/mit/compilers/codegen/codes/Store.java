package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.dataflow.operand.Operand;

import java.util.Optional;
import java.util.Set;

public abstract class Store extends Instruction implements Cloneable {
    public AssignableName store;

    public Store(AssignableName store, AST source) {
        super(source);
        this.store = store;
    }

    public Store(AssignableName store, AST source, String comment) {
        super(source, comment);
        this.store = store;
    }

    public AssignableName getStore() {
        return store;
    }

    public void setStore(AssignableName dst) {
        this.store = dst;
    }

    public abstract Optional<Operand> getOperandNoArray();

    public Optional<Operand> getOperandNoArrayNoGlobals(Set<AbstractName> globals) {
        Optional<Operand> operand = getOperandNoArray();
        if (operand.isPresent()) {
            if (operand.get().containsAny(globals)) {
                return Optional.empty();
            }
        }
        return operand;
    }
    @Override
    public Store clone() {
        try {
            Store clone = (Store) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            clone.store = store;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
