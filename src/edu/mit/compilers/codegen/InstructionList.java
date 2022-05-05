package edu.mit.compilers.codegen;

import edu.mit.compilers.codegen.codes.ArrayAccess;
import edu.mit.compilers.codegen.codes.Instruction;
import edu.mit.compilers.codegen.names.*;

import java.util.*;
import java.util.stream.Collectors;

public class InstructionList extends ArrayList<Instruction> {
    public AbstractName place;
    public InstructionList nextInstructionList;
    public InstructionList tailInstructionList;

    public InstructionList(AbstractName place, List<Instruction> codes) {
        this.place = place;
        addAll(codes);
    }

    public InstructionList(List<Instruction> codes) {
        this(null, codes);
    }

    public InstructionList(AbstractName place) {
        this(place, Collections.emptyList());
    }

    public InstructionList() {
        this(null, Collections.emptyList());
    }

    public int flattenedSize() {
        return flatten().size();
    }

    public Optional<InstructionList> getNextInstructionList() {
        return Optional.ofNullable(nextInstructionList);
    }

    public Optional<Instruction> lastCode() {
        if (isEmpty())
            return Optional.empty();
        return Optional.of(get(size() - 1));
    }

    public InstructionList flatten() {
        InstructionList flattened = new InstructionList();
        InstructionList tacList = this;
        while (tacList != null) {
            flattened.addAll(tacList);
            tacList = tacList.nextInstructionList;
        }
        return flattened;
    }

    public void reset(Collection<Instruction> newCodes) {
        clear();
        addAll(newCodes);
    }

    public void replaceIfContainsOldCodeAtIndex(int indexOfOldCode, Instruction oldCode, Instruction newCode) {
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

    public static InstructionList of(Instruction code, AssignableName place) {
        var instructionList = new InstructionList(place);
        instructionList.add(code);
        return instructionList;
    }

    public Instruction lastCodeInLinkedList() {
        InstructionList flattened = flatten();
        return flattened.get(flattened.size() - 1);
    }

    public Instruction firstCode() {
        return get(0);
    }

    public InstructionList appendInInstructionListLinkedList(InstructionList instructionList) {
        InstructionList head = this;
        while (head.nextInstructionList != null)
            head = head.nextInstructionList;
        head.nextInstructionList = instructionList;
        return instructionList;
    }

    public void addToTail(Instruction tac)  {
        var head = this;
        while (head
                .getNextInstructionList()
                .isPresent())
            head = head
                    .getNextInstructionList()
                    .get();
        head.add(tac);
    }

    @Override
    public String toString() {
        return flatten().stream().filter(code -> !(code instanceof ArrayAccess)).map(Instruction::repr).collect(Collectors.joining("\n"));
    }
    public InstructionList copy() {
        return new InstructionList(place, this);
    }
}
