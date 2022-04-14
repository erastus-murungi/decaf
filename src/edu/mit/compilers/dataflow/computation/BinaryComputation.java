package edu.mit.compilers.dataflow.computation;

import edu.mit.compilers.codegen.codes.HasResult;
import edu.mit.compilers.codegen.codes.Quadruple;
import edu.mit.compilers.codegen.names.AbstractName;

import java.util.Collection;
import java.util.Objects;


public class BinaryComputation extends Computation {
    public final AbstractName fstOperand;
    public final String operator;
    public final AbstractName sndOperand;

    public BinaryComputation(Quadruple quadruple) {
        this.fstOperand = quadruple.fstOperand;
        this.operator = quadruple.operator;
        this.sndOperand = quadruple.sndOperand;
    }

    @Override
    public boolean contains(AbstractName name) {
        return this.fstOperand.equals(name) || this.sndOperand.equals(name);
    }

    @Override
    public boolean isContainedIn(HasResult hasResult) {
        if (hasResult instanceof Quadruple) {
            Quadruple quadruple = (Quadruple) hasResult;
            return new BinaryComputation(quadruple).equals(this);
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BinaryComputation that = (BinaryComputation) o;

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
