package edu.mit.compilers.registerallocation;

import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.names.AssignableName;

public class LiveInterval {
    private InstructionList instructionList;
    AssignableName variable;
    int startPoint;
    int endPoint;
    String methodName;

    public InstructionList getInstructionList() {
        return instructionList;
    }

    public LiveInterval(AssignableName variable, int startPoint, int endPoint, InstructionList instructionList, String methodName) {
        this.variable = variable;
        this.startPoint = startPoint;
        this.endPoint = endPoint;
        this.instructionList = instructionList;
        this.methodName = methodName;
    }

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

    @Override
    public String toString() {
        return "LiveInterval{" +
                "variable=" + variable.repr() +
                ", start=" + startPoint +
                ", end=" + endPoint +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}
