package edu.mit.compilers.registerallocation;

import edu.mit.compilers.codegen.InstructionList;
import edu.mit.compilers.codegen.names.AssignableName;

public class LiveInterval {
    InstructionList instructionList;
    AssignableName variable;
    int startPoint;
    int end;
    String methodName;

    public LiveInterval(AssignableName variable, int startPoint, int endPoint, InstructionList instructionList, String methodName) {
        this.variable = variable;
        this.startPoint = startPoint;
        this.end = endPoint;
        this.instructionList = instructionList;
        this.methodName = methodName;
    }

    @Override
    public String toString() {
        return "LiveInterval{" +
                "variable=" + variable.repr() +
                ", start=" + startPoint +
                ", end=" + end +
                ", methodName='" + methodName + '\'' +
                '}';
    }
}
