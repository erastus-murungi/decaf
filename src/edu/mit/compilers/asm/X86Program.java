package edu.mit.compilers.asm;

import static com.google.common.base.Preconditions.checkState;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import edu.mit.compilers.asm.instructions.X64Instruction;
import edu.mit.compilers.asm.instructions.X86MetaData;

public class X86Program extends ArrayList<X64Instruction> {

    public X86Program() {
    }

    public X86Program(List<X64Instruction> prologue,
                      List<X64Instruction> epilogue,
                      List<X86Method> x86Methods
    ) {
        addAll(prologue);
        x86Methods.forEach(this::addAll);
        addAll(epilogue);
    }

    public void addPrologue(List<X86MetaData> prologue) {
        addAll(prologue);
    }
    public void addEpilogue(List<X86MetaData> epilogue) {
        addAll(epilogue);
    }

    public void addMethod(X86Method x86Method) {
        addAll(x86Method);
    }

    @Override
    public String toString() {
        return stream().map(X64Instruction::toString).collect(Collectors.joining("\n")) + "\n";
    }
}
