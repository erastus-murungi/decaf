package edu.mit.compilers.utils;

import java.util.List;

import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.codes.MethodBegin;

public class ProgramIr {
    public InstructionList headerInstructions;
    public List<MethodBegin> methodBeginList;

    public ProgramIr(InstructionList headerInstructions, List<MethodBegin> methodBeginList) {
        this.headerInstructions = headerInstructions;
        this.methodBeginList = methodBeginList;
    }

    public int getSizeOfHeaderInstructions() {
        return headerInstructions.size();
    }

    public InstructionList mergeProgram() {
        var programHeader = headerInstructions.copy();
        var tacList = programHeader.copy();
        for (MethodBegin methodBegin : methodBeginList) {
            tacList.addAll(methodBegin.entryBlock.instructionList.flatten());
        }
        return tacList;
    }
}
