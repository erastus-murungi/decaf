package edu.mit.compilers.codegen.codes;

import java.util.List;

import edu.mit.compilers.asm.AsmWriter;
import edu.mit.compilers.codegen.names.LValue;
import edu.mit.compilers.codegen.names.MemoryAddress;
import edu.mit.compilers.codegen.names.Value;
import edu.mit.compilers.dataflow.operand.Operand;
import edu.mit.compilers.utils.Utils;

public class ArrayBoundsCheck extends HasOperand {
    public GetAddress getAddress;
    public Integer boundsIndex;
    private MemoryAddress destination;
    private Value index;
    private LValue baseAddress;

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
    public List<Value> getAllValues() {
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
    public List<Value> getOperandValues() {
        return List.of(index, baseAddress);
    }

    @Override
    public boolean replaceValue(Value oldName, Value newName) {
        var changesHappened = false;
        if (index == oldName) {
            index = newName;
            changesHappened = true;
        }
        if (baseAddress == oldName) {
            oldName = newName;
            changesHappened = true;
        }
        return changesHappened;
    }

    @Override
    public String toString() {
        return String.format("%s%s %s %s, %s, %s", DOUBLE_INDENT, "checkbounds", destination.toString(), baseAddress.getType().getColoredSourceCode(), index.repr(), getAddress.getLength().orElseThrow().repr());
    }

    public String syntaxHighlightedToString() {
        final var checkBoundsString = Utils.coloredPrint("checkbounds", Utils.ANSIColorConstants.ANSI_GREEN_BOLD);
        return String.format("%s%s %s %s, %s, %s", DOUBLE_INDENT, checkBoundsString, destination.toString(), baseAddress.getType().getColoredSourceCode(), index.repr(), getAddress.getLength().orElseThrow().repr());
    }

}