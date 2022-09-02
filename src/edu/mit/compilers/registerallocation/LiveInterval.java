package edu.mit.compilers.registerallocation;

import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.names.LValue;

public record LiveInterval(LValue variable, int startPoint, int endPoint, InstructionList instructionList, String methodName) {
    public int compareStartPoint(LiveInterval other) {
        if(startPoint == other.startPoint)
            return 0;
        return startPoint < other.startPoint ? -1 : 1;
    }

    public int compareEndpoint(LiveInterval other) {
        if(endPoint == other.endPoint)
            return 0;
        return endPoint < other.endPoint ? -1 : 1;
    }
}
