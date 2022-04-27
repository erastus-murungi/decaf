package edu.mit.compilers.dataflow.operand;

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
}
