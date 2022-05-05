package edu.mit.compilers.dataflow.operand;

import edu.mit.compilers.codegen.codes.Store;
import edu.mit.compilers.codegen.codes.BinaryInstruction;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;


public class BinaryOperand extends Operand {
    public final AbstractName fstOperand;
    public final String operator;
    public final AbstractName sndOperand;

    public BinaryOperand(BinaryInstruction binaryInstruction) {
        this.fstOperand = binaryInstruction.fstOperand;
        this.operator = binaryInstruction.operator;
        this.sndOperand = binaryInstruction.sndOperand;
    }

    @Override
    public boolean contains(AbstractName name) {
        return this.fstOperand.equals(name) || this.sndOperand.equals(name);
    }

    @Override
    public boolean isContainedIn(Store store) {
        if (store instanceof BinaryInstruction) {
            BinaryInstruction binaryInstruction = (BinaryInstruction) store;
            return new BinaryOperand(binaryInstruction).equals(this);
        }
        return false;
    }

    @Override
    public Store storeInstructionFromOperand(AssignableName store) {
        return new BinaryInstruction(store, fstOperand, operator, sndOperand, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BinaryOperand that = (BinaryOperand) o;

        if (operator.equals(that.operator)) {
            final String operator = that.operator;
            boolean operandsExactlyEqual = fstOperand.equals(that.fstOperand) && sndOperand.equals(that.sndOperand);
            if (operatorIsCommutative(operator)) {
                return operandsExactlyEqual ||
                        sndOperand.equals(that.fstOperand) && fstOperand.equals(that.sndOperand);
            } else {
                return operandsExactlyEqual;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return fstOperand.hashCode() ^ sndOperand.hashCode() ^ operator.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", fstOperand, operator, sndOperand);
    }
}
