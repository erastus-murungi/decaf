package decaf.ir.dataflow.operand;


import java.util.List;
import java.util.Objects;

import decaf.ir.names.IrValue;

public class GetAddressOperand extends Operand {
  private final GetAddress getAddress;

  public GetAddressOperand(GetAddress getAddress) {
    this.getAddress = getAddress;
  }

  @Override
  public boolean contains(IrValue comp) {
    return getAddress.getBaseAddress()
                     .equals(comp) || getAddress.getIndex()
                                                .equals(comp);
  }

  @Override
  public List<IrValue> getNames() {
    return getAddress.genOperandIrValuesSurface();
  }

  @Override
  public boolean isContainedIn(StoreInstruction storeInstruction) {
    if (storeInstruction instanceof GetAddress otherGetAddress) {
      return getAddress.equals(otherGetAddress);
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    GetAddressOperand that = (GetAddressOperand) o;
    return Objects.equals(
        getAddress,
        that.getAddress
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(getAddress);
  }
}
