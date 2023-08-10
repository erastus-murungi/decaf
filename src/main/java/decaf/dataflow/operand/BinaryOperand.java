package decaf.dataflow.operand;


import java.util.List;

import decaf.codegen.codes.BinaryInstruction;
import decaf.codegen.codes.StoreInstruction;
import decaf.codegen.names.IrValue;


public class BinaryOperand extends Operand {
  public final IrValue fstOperand;
  public final String operator;
  public final IrValue sndOperand;

  public BinaryOperand(BinaryInstruction binaryInstruction) {
    this.fstOperand = binaryInstruction.fstOperand;
    this.operator = binaryInstruction.operator;
    this.sndOperand = binaryInstruction.sndOperand;
  }

  @Override
  public boolean contains(IrValue name) {
    return this.fstOperand.equals(name) || this.sndOperand.equals(name);
  }

  @Override
  public List<IrValue> getNames() {
    return List.of(
        fstOperand,
        sndOperand
    );
  }

  @Override
  public boolean isContainedIn(StoreInstruction storeInstruction) {
    if (storeInstruction instanceof BinaryInstruction binaryInstruction) {
      return new BinaryOperand(binaryInstruction).equals(this);
    }
    return false;
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
    return String.format(
        "%s %s %s",
        fstOperand,
        operator,
        sndOperand
    );
  }
}
