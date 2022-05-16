package edu.mit.compilers.codegen.codes;

import java.util.List;

import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.utils.Utils;

public class ArrayBoundsCheck extends Instruction implements HasOperand {
    public GetAddress getAddress;
    public Integer boundsIndex;

    public ArrayBoundsCheck(GetAddress getAddress, Integer boundsIndex) {
        super(null);
        this.getAddress = getAddress;
        this.boundsIndex = boundsIndex;
    }

    public String getIndexIsLessThanArraySizeLabel() {
        return "index_less_than_array_length_check_done_" + boundsIndex;
    }

    public String getIndexIsNonNegativeLabel() {
        return "index_non_negative_check_done_" + boundsIndex;
    }

    @Override
    public <T, E> T accept(InstructionVisitor<T, E> visitor, E extra) {
        return visitor.visit(this, extra);
    }

    @Override
    public List<AbstractName> getAllNames() {
        return getAddress.getAllNames();
    }

    @Override
    public String repr() {
        return toString();
    }

    @Override
    public Instruction copy() {
        return new ArrayBoundsCheck(getAddress, boundsIndex);
    }

    @Override
    public String toString() {
        final var checkBoundsString = Utils.coloredPrint("checkbounds", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        return String.format("%s%s %s %s, %s, %s", DOUBLE_INDENT, checkBoundsString, getAddress.getStore().toString(), getAddress.getBaseAddress().builtinType.getColoredSourceCode(), getAddress.getIndex().repr(), getAddress.getLength().orElseThrow().repr());
    }

    @Override
    public Operand getOperand() {
        return getAddress.getOperand();
    }

    @Override
    public List<AbstractName> getOperandNames() {
        return getAddress.getOperandNames();
    }

    @Override
    public boolean replace(AbstractName oldName, AbstractName newName) {
        return getAddress.replace(oldName, newName);
    }
}