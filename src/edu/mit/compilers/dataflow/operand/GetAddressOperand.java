package edu.mit.compilers.dataflow.operand;

import java.util.Objects;

import edu.mit.compilers.codegen.codes.GetAddress;
import edu.mit.compilers.codegen.codes.Store;
import edu.mit.compilers.codegen.names.AbstractName;
import edu.mit.compilers.codegen.names.AssignableName;

public class GetAddressOperand extends Operand {
    private final GetAddress getAddress;

    public GetAddressOperand(GetAddress getAddress) {
        this.getAddress = getAddress;
    }

    @Override
    public boolean contains(AbstractName comp) {
        return getAddress.getBaseAddress().equals(comp) || getAddress.getIndex().equals(comp);
    }

    @Override
    public boolean isContainedIn(Store store) {
        if (store instanceof GetAddress) {
            GetAddress otherGetAddress = (GetAddress) store;
            return getAddress.equals(otherGetAddress);
        }
        return false;
    }

    @Override
    public Store getStoreInstructionFromOperand(AssignableName store) {
        return new GetAddress(getAddress.source, getAddress.getBaseAddress(), getAddress.getIndex(), getAddress.getStore(), getAddress.getLength().orElse(null));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GetAddressOperand that = (GetAddressOperand) o;
        return Objects.equals(getAddress, that.getAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAddress);
    }
}
