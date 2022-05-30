package edu.mit.compilers.dataflow.operand;

import java.util.Objects;

import edu.mit.compilers.codegen.codes.StoreInstruction;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.ssa.Phi;

public class PhiOperand extends Operand {
    private final Phi phi;

    public Phi getPhi() {
        return phi;
    }

    public PhiOperand(Phi phi) {
        this.phi = phi;
    }

    @Override
    public boolean contains(AbstractName comp) {
        return phi.getOperandNames()
                  .contains(comp);
    }

    @Override
    public boolean isContainedIn(StoreInstruction storeInstruction) {
        if (storeInstruction instanceof Phi) {
            return new PhiOperand((Phi) storeInstruction).equals(this);
        }
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhiOperand that = (PhiOperand) o;
        return Objects.equals(getPhi().getOperandNames(), that.getPhi().getOperandNames());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPhi().getOperandNames());
    }
}
