package edu.mit.compilers.codegen;

import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.names.*;

import java.util.*;
import java.util.stream.Collectors;

public class InstructionList extends ArrayList<Instruction> {
    public AbstractName place;

    public InstructionList(AbstractName place, List<Instruction> codes) {
        this.place = place;
        addAll(codes);
    }

    public InstructionList(List<Instruction> instructions) {
        this(null, instructions);
    }

    public InstructionList(AbstractName place) {
        this(place, Collections.emptyList());
    }

    public InstructionList() {
        this(null, Collections.emptyList());
    }

    public Optional<Instruction> lastInstruction() {
        if (isEmpty())
            return Optional.empty();
        return Optional.of(get(size() - 1));
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


    public static InstructionList of(Instruction code) {
        var instructionList = new InstructionList();
        instructionList.add(code);
        return instructionList;
    }

    public static InstructionList of(Instruction instruction, AssignableName place) {
        var instructionList = new InstructionList(place);
        instructionList.add(instruction);
        return instructionList;
    }

    public Optional<Instruction> firstInstruction() {
        if (isEmpty())
            return Optional.empty();
        return Optional.of(get(0));
    }

    @Override
    public String toString() {
        return stream()
                .map(Instruction::repr)
                .collect(Collectors.joining("\n"));
    }

    public InstructionList copy() {
        return new InstructionList(place, this);
    }
}
