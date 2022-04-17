package edu.mit.compilers.dataflow.operand;

import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.codegen.codes.MethodCallSetResult;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;

import java.util.Objects;

public class MethodCallOperand extends Operand {
    public MethodCallSetResult methodCallSetResult;

    public MethodCallOperand(MethodCallSetResult methodCallSetResult) {
        this.methodCallSetResult = methodCallSetResult;
    }

    @Override
    public boolean contains(AbstractName comp) {
        return false;
    }

    @Override
    public boolean isContainedIn(HasResult hasResult) {
        return false;
    }

    @Override
    public HasResult fromOperand(AssignableName resultLocation) {
        var methodCallSetResultCopy = methodCallSetResult.clone();
        methodCallSetResultCopy.setResultLocation(resultLocation);
        return methodCallSetResultCopy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodCallOperand that = (MethodCallOperand) o;
        return Objects.equals(methodCallSetResult.toString(), that.methodCallSetResult.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodCallSetResult.toString());
    }

    @Override
    public String toString() {
        return methodCallSetResult.toString();
    }
}
