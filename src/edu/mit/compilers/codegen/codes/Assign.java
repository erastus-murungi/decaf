package edu.mit.compilers.codegen.codes;

import edu.mit.compilers.ast.AST;
import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;
import edu.mit.compilers.codegen.names.MemoryAddressName;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.dataflow.operand.UnmodifiedOperand;
import edu.mit.compilers.utils.Utils;

import java.util.List;
import java.util.Optional;

public class Assign extends Store implements Cloneable, HasOperand {
    public AbstractName operand;

    public Assign(AssignableName dst, AbstractName operand, AST source, String comment) {
        super(dst, source, comment);
        this.operand = operand;
    }

    public static Assign ofRegularAssign(AssignableName dst, AbstractName operand) {
        return new Assign(dst, operand, null, "");
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getAllNames() {
        return List.of(store, operand);
    }

    @Override
    public String repr() {
        var storeString = Utils.coloredPrint("store", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        return String.format("%s%s %s, %s", DOUBLE_INDENT, storeString, operand.repr(), store.repr());
    }

    @Override
    public Instruction copy() {
        return new Assign(store, operand, source, getComment().orElse(null));
    }

    @Override
    public String toString() {
        return repr();
    }

    @Override
    public Optional<Operand> getOperandNoArray() {
        if (operand instanceof MemoryAddressName)
            return Optional.empty();
        return Optional.of(new UnmodifiedOperand(operand));
    }

    public boolean contains(AbstractName name) {
        return store.equals(name) || operand.equals(name);
    }

    @Override
    public Assign clone() {
        Assign clone = (Assign) super.clone();
        // TODO: copy mutable state here, so the clone can't change the internals of the original
        clone.operand = operand;
        clone.setComment(getComment().orElse(null));
        clone.store = store;
        clone.source = source;
        return clone;
    }

    @Override
    public Operand getOperand() {
        return new UnmodifiedOperand(operand);
    }

    @Override
    public List<AbstractName> getOperandNames() {
        return List.of(operand);
    }

    public boolean replace(AbstractName oldVariable, AbstractName replacer) {
        var replaced = false;
        if (operand.equals(oldVariable)) {
            operand = replacer;
            replaced = true;
        }
        return replaced;
    }

}
