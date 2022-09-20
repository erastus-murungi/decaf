package decaf.codegen.codes;

import java.util.Optional;
import java.util.Set;

import decaf.ast.AST;
import decaf.codegen.names.IrAssignable;
import decaf.codegen.names.IrMemoryAddress;
import decaf.codegen.names.IrSsaRegister;
import decaf.codegen.names.IrValue;
import decaf.common.Utils;
import decaf.dataflow.operand.Operand;

public abstract class StoreInstruction extends HasOperand implements Cloneable {
    protected IrAssignable destination;

    public StoreInstruction(IrAssignable destination, AST source) {
        super(source);
        this.destination = destination;
    }

    public StoreInstruction(IrAssignable destination) {
        super();
        this.destination = destination;
    }

    public StoreInstruction(IrAssignable destination, AST source, String comment) {
        super(source, comment);
        this.destination = destination;
    }

    public IrAssignable getDestination() {
        return destination;
    }

    public void setDestination(IrAssignable dst) {
        this.destination = dst;
    }

    public abstract Optional<Operand> getOperandNoArray();

    public Optional<Operand> getOperandNoArrayNoGlobals(Set<IrValue> globals) {
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

    public String getPrefix() {
        if (getDestination() instanceof IrMemoryAddress) {
            return "setmem";
        } else {
            return "setreg";
        }
    }

    public String getPrefixSyntaxHighlighted() {
        return Utils.coloredPrint(getPrefix(), Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
    }
}
