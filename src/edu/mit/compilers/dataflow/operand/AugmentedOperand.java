package edu.mit.compilers.dataflow.operand;

import java.util.Objects;

import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;

public class AugmentedOperand extends Operand {
    public String operator;
    public AbstractName operand;

    public AugmentedOperand(String operator, AbstractName operand) {
        this.operator = operator;
        this.operand = operand;
    }

    @Override
    public boolean contains(AbstractName name) {
        return this.operand.equals(name);
    }

    @Override
    public boolean isContainedIn(HasResult hasResult) {
        return false;
    }

    @Override
    public HasResult fromOperand(AssignableName resultLocation) {
        return new Assign(resultLocation, operator, operand, null, null);
    }

    @Override
    public String toString() {
        return  operator + operand;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AugmentedOperand that = (AugmentedOperand) o;
        return Objects.equals(operator, that.operator) && Objects.equals(operand, that.operand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operator, operand);
    }
}
