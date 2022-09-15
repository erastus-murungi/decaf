package decaf.codegen.codes;

import java.util.Optional;
import java.util.Set;

import decaf.ast.AST;
import decaf.codegen.names.IrGlobal;
import decaf.codegen.names.IrAssignableValue;
import decaf.dataflow.operand.Operand;

public abstract class StoreInstruction extends HasOperand implements Cloneable {
    protected IrAssignableValue destination;

    public StoreInstruction(IrAssignableValue destination, AST source) {
        super(source);
        this.destination = destination;
    }

    public StoreInstruction(IrAssignableValue destination) {
        super();
        this.destination = destination;
    }

    public StoreInstruction(IrAssignableValue destination, AST source, String comment) {
        super(source, comment);
        this.destination = destination;
    }

    public IrAssignableValue getDestination() {
        return destination;
    }

    public void setDestination(IrAssignableValue dst) {
        this.destination = dst;
    }

    public abstract Optional<Operand> getOperandNoArray();

    public Optional<Operand> getOperandNoArrayNoGlobals(Set<IrGlobal> globals) {
        Optional<Operand> operand = getOperandNoArray();
        if (operand.isPresent()) {
            if (operand.get().getNames().stream().anyMatch(globals::contains))
                return Optional.empty();
            }
        return operand;
    }

    @Override
    public StoreInstruction clone() {
        try {
            StoreInstruction clone = (StoreInstruction) super.clone();
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            clone.destination = destination;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
