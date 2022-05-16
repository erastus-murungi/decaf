package edu.mit.compilers.dataflow.operand;

import java.util.Objects;

import edu.mit.compilers.codegen.codes.Assign;
import edu.mit.compilers.codegen.codes.Store;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;

public class IncDecOperand extends Operand {
    private String operator;
    private AssignableName operand;

    public IncDecOperand(String operator, AssignableName operand) {
        this.operator = operator;
        this.operand = operand;
    }

    @Override
    public boolean contains(AbstractName name) {
        return this.operand.equals(name);
    }

    @Override
    public boolean isContainedIn(Store store) {
        return false;
    }

    @Override
    public Store getStoreInstructionFromOperand(AssignableName store) {
        return new Assign(store, operator, operand, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IncDecOperand that = (IncDecOperand) o;
        return Objects.equals(operator, that.operator) && Objects.equals(operand, that.operand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operator, operand);
    }
}
