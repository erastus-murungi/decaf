package edu.mit.compilers.asm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.mit.compilers.asm.instructions.X64Instruction;
import edu.mit.compilers.asm.instructions.X64MetaData;

public class X64Method extends ArrayList<X64Instruction> {
    private final List<X64Instruction> prologue = new ArrayList<>();
    private final List<X64Instruction> epilogue = new ArrayList<>();

    public X64Method() {
    }

    public X64Method addPrologue(X64MetaData x64MetaData) {
        prologue.add(x64MetaData);
        return this;
    }

    public X64Method addEpilogue(X64MetaData x64MetaData) {
        epilogue.add(x64MetaData);
        return this;
    }

    public X64Method addLine(X64Instruction x64Instruction) {
        super.add(x64Instruction);
        return this;
    }

    public X64Method addAtIndex(int index, X64Instruction line) {
        add(index, line);
        return this;
    }

    public void addAllAtIndex(int index, Collection<X64Instruction> lines) {
        addAll(index, lines);
    }

    public X64Method addLines(Collection<X64Instruction> lines) {
        addAll(lines);
        return this;
    }

    private Stream<X64Instruction> buildStream() {
        return Stream.of(prologue, this, epilogue).flatMap(Collection::stream);
    }

    @Override
    public String toString() {
        var elements = buildStream().toList();
        return buildStream().map(X64Instruction::toString).collect(Collectors.joining("\n"));
    }

}
