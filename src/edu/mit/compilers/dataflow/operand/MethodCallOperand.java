package edu.mit.compilers.dataflow.operand;

import java.util.Objects;

import edu.mit.compilers.codegen.codes.FunctionCall;
import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.Value;

public class MethodCallOperand extends Operand {
    public FunctionCall functionCallWithResult;

    public MethodCallOperand(FunctionCall functionCallWithResult) {
        this.functionCallWithResult = functionCallWithResult;
    }

    @Override
    public boolean contains(Value comp) {
        return false;
    }

    @Override
    public boolean isContainedIn(StoreInstruction storeInstruction) {
        return false;
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
