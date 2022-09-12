package edu.mit.compilers.codegen;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.names.VirtualRegister;
import edu.mit.compilers.codegen.names.Value;

public class InstructionList extends ArrayList<Instruction> {
    public Value place;

    private Integer labelIndex;

    private String label;

    private boolean isEntry = false;

    public InstructionList(Value place, List<Instruction> codes) {
        this.place = place;
        this.label = "UNSET";
        addAll(codes);
    }

    public InstructionList(List<Instruction> instructions) {
        this(null, instructions);
    }

    public InstructionList(Value place) {
        this(place, Collections.emptyList());
    }

    public InstructionList() {
        this(null, Collections.emptyList());
    }

    public static InstructionList of(Instruction code) {
        var instructionList = new InstructionList();
        instructionList.add(code);
        return instructionList;
    }

    public static InstructionList of(Instruction instruction, VirtualRegister place) {
        var instructionList = new InstructionList(place);
        instructionList.add(instruction);
        return instructionList;
    }

    public void setEntry() {
        isEntry = true;
    }

    public boolean isEntry() {
        return isEntry;
    }

    public String getLabel() {
        if (this.label == null)
            return "L" + labelIndex;
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setLabel(int labelIndex) {
        this.labelIndex = labelIndex;
        this.label = null;
    }

    public String getLabelForAsm() {
        if (this.label == null)
            return "L" + labelIndex;
        return label;
    }

    public void reset(Collection<Instruction> newCodes) {
        clear();
        addAll(newCodes);
    }

    public void replaceIfContainsInstructionAtIndex(int indexOfOldCode, Instruction oldCode, Instruction newCode) {
        if (get(indexOfOldCode) != oldCode) {
            throw new IllegalArgumentException(oldCode + "not found in Instruction List");
        }
        set(indexOfOldCode, newCode);
    }

    @Override
    public String toString() {
        return stream()
                .map(Instruction::syntaxHighlightedToString)
                .collect(Collectors.joining("\n"));
    }

    public InstructionList copy() {
        return new InstructionList(place, this);
    }
}
