package edu.mit.compilers.utils;

import java.util.List;

import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.MethodBegin;

public class ProgramIr {
    public InstructionList instructionList;
    public List<MethodBegin> methodBeginList;

    public ProgramIr(InstructionList instructionList, List<MethodBegin> methodBeginList) {
        this.instructionList = instructionList;
        this.methodBeginList = methodBeginList;
    }

    public InstructionList mergeProgram() {
        var programHeader = instructionList.copy();
        var tacList = programHeader.copy();
        for (MethodBegin methodBegin : methodBeginList) {
            tacList.addAll(methodBegin.entryBlock.instructionList.flatten());
        }
        return tacList;
    }
}
