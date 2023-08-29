package decaf.ir.dataflow.operand;


import java.util.List;
import java.util.Objects;

import decaf.ir.names.IrValue;

public class UnaryOperand extends Operand {
  public final IrValue operand;
  public final String operator;

  public UnaryOperand(UnaryInstruction unaryInstruction) {
    this.operand = unaryInstruction.operand;
    this.operator = unaryInstruction.operator;
  }

  @Override
  public boolean contains(IrValue name) {
    return this.operand.equals(name);
  }

  @Override
  public List<IrValue> getNames() {
    return List.of(operand);
  }

  @Override
  public boolean isContainedIn(StoreInstruction storeInstruction) {
    if (storeInstruction instanceof UnaryInstruction unaryInstruction) {
      return new UnaryOperand(unaryInstruction).equals(this);
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UnaryOperand that = (UnaryOperand) o;
    return Objects.equals(
        operand,
        that.operand
    ) && Objects.equals(
        operator,
        that.operator
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        operand,
        operator
    );
  }

  @Override
  public String toString() {
    return String.format(
        "%s %s",
        operator,
        operand
    );
  }
}
