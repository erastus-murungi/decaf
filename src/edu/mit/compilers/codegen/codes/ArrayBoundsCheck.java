package edu.mit.compilers.codegen.codes;

import java.util.List;

import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.codegen.names.IrValue;
import edu.mit.compilers.codegen.names.IrAssignableValue;
import edu.mit.compilers.codegen.names.IrMemoryAddress;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.utils.Utils;

public class ArrayBoundsCheck extends HasOperand {
    public GetAddress getAddress;
    public Integer boundsIndex;
    private IrMemoryAddress destination;
    private IrValue index;
    private IrValue baseAddress;

    public ArrayBoundsCheck(GetAddress getAddress, Integer boundsIndex) {
        super(null);
        this.getAddress = getAddress;
        this.boundsIndex = boundsIndex;
        this.destination = getAddress.getDestination().copy();
        this.index = getAddress.getIndex().copy();
        this.baseAddress = getAddress.getBaseAddress().copy();
    }

    public String getIndexIsLessThanArraySizeLabel() {
        return "index_less_than_array_length_check_done_" + boundsIndex;
    }

    public String getIndexIsNonNegativeLabel() {
        return "index_non_negative_check_done_" + boundsIndex;
    }

    @Override
    public void accept(AsmWriter asmWriter) {
        asmWriter.emitInstruction(this);
    }

    @Override
    public List<IrValue> getAllValues() {
        return getAddress.getOperandValues();
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
    public List<IrValue> getOperandValues() {
        return List.of(index, baseAddress);
    }

    @Override
    public boolean replaceValue(IrValue oldName, IrValue newName) {
        var changesHappened = false;
        if (index == oldName) {
            index = newName;
            changesHappened = true;
        }
        if (baseAddress == oldName) {
            baseAddress = newName;
            changesHappened = true;
        }
        return changesHappened;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s %s, %s, %s", DOUBLE_INDENT, "checkbounds", destination.toString(), baseAddress.getType().getColoredSourceCode(), index, getAddress.getLength());
    }

    public String syntaxHighlightedToString() {
        final var checkBoundsString = Utils.coloredPrint("checkbounds", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        return String.format("%s%s %s %s, %s, %s", DOUBLE_INDENT, checkBoundsString, destination.toString(), baseAddress.getType().getColoredSourceCode(), index, getAddress.getLength());
    }

}