package edu.mit.compilers.dataflow.operand;

import edu.mit.compilers.codegen.codes.Store;
import edu.mit.compilers.codegen.codes.FunctionCallWithResult;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;

import java.util.Objects;

public class MethodCallOperand extends Operand {
    public FunctionCallWithResult functionCallWithResult;

    public MethodCallOperand(FunctionCallWithResult functionCallWithResult) {
        this.functionCallWithResult = functionCallWithResult;
    }

    @Override
    public boolean contains(AbstractName comp) {
        return false;
    }

    @Override
    public boolean isContainedIn(Store store) {
        return false;
    }

    @Override
    public Store getStoreInstructionFromOperand(AssignableName store) {
        var methodCallSetResultCopy = functionCallWithResult.clone();
        methodCallSetResultCopy.setStore(store);
        return methodCallSetResultCopy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodCallOperand that = (MethodCallOperand) o;
        return Objects.equals(functionCallWithResult.toString(), that.functionCallWithResult.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionCallWithResult.toString());
    }

    @Override
    public String toString() {
        return functionCallWithResult.toString();
    }
}
