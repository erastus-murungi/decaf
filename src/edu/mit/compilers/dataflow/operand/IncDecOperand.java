package edu.mit.compilers.dataflow.operand;

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
    public Store storeInstructionFromOperand(AssignableName store) {
        return new Assign(store, operator, operand, null, null);
    }
}
