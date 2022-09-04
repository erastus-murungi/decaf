package edu.mit.compilers.codegen.codes;

import java.util.List;

import edu.mit.compilers.codegen.InstructionVisitor;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.utils.Utils;

public class ArrayBoundsCheck extends HasOperand {
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
    public List<Value> getAllValues() {
        return getAddress.getAllValues();
    }

    @Override
    public Instruction copy() {
        return new ArrayBoundsCheck(getAddress, boundsIndex);
    }


    @Override
    public Operand getOperand() {
        return getAddress.getOperand();
    }

    @Override
    public List<Value> getOperandValues() {
        return getAddress.getOperandValues();
    }

    @Override
    public boolean replace(Value oldName, Value newName) {
        return getAddress.replace(oldName, newName);
    }

    @Override
    public String toString() {
        return String.format("%s%s %s %s, %s, %s", DOUBLE_INDENT, "checkbounds", getAddress.getDestination().toString(), getAddress.getBaseAddress().getType().getColoredSourceCode(), getAddress.getIndex().repr(), getAddress.getLength().orElseThrow().repr());
    }

    public String syntaxHighlightedToString() {
        final var checkBoundsString = Utils.coloredPrint("checkbounds", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        return String.format("%s%s %s %s, %s, %s", DOUBLE_INDENT, checkBoundsString, getAddress.getDestination().toString(), getAddress.getBaseAddress().getType().getColoredSourceCode(), getAddress.getIndex().repr(), getAddress.getLength().orElseThrow().repr());
    }

}