package edu.mit.compilers.asm;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import edu.mit.compilers.asm.instructions.X64Instruction;

public class X64Program extends ArrayList<X64Instruction> {
    public X64Program(List<X64Instruction> prologue,
                      List<X64Instruction> epilogue,
                      List<X64Method> x64Methods) {
        addAll(prologue);
        x64Methods.forEach(this::addAll);
        addAll(epilogue);
    }

    @Override
    public String toString() {
        return stream().map(X64Instruction::toString).collect(Collectors.joining("\n")) + "\n";
    }
}
