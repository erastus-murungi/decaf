package decaf.dataflow.operand;


import java.util.List;
import java.util.Objects;

import decaf.codegen.codes.StoreInstruction;
import decaf.codegen.names.IrValue;
import decaf.grammar.Scanner;

public class UnmodifiedOperand extends Operand {
  public IrValue irValue;
  public String operator;

  public UnmodifiedOperand(IrValue irValue) {
    this.irValue = irValue;
    this.operator = Scanner.ASSIGN;
  }

  @Override
  public boolean contains(IrValue comp) {
    return comp.equals(irValue);
  }

  @Override
  public List<IrValue> getNames() {
    return List.of(irValue);
  }

  @Override
  public boolean isContainedIn(StoreInstruction storeInstruction) {
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UnmodifiedOperand that = (UnmodifiedOperand) o;
    return Objects.equals(
        irValue,
        that.irValue
    );
  }

  @Override
  public int hashCode() {
    return irValue.hashCode();
  }

  @Override
  public String toString() {
    return irValue.toString();
  }
}
