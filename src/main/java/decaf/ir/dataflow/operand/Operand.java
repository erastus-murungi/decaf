package decaf.ir.dataflow.operand;


import java.util.List;
import java.util.Set;

import decaf.analysis.lexical.Scanner;
import decaf.ir.names.IrValue;

public abstract class Operand {
  private static int indexCounter;
  private final int index;

  public Operand() {
    this.index = indexCounter++;
  }

  public static boolean operatorIsCommutative(String operator) {
    return operator.equals(Scanner.PLUS) || operator.equals(Scanner.MULTIPLY);
  }

  public abstract boolean contains(IrValue comp);

  public boolean containsAny(Set<IrValue> names) {
    return names.stream()
                .anyMatch(this::contains);
  }

  public int getIndex() {
    return index;
  }

  public abstract List<IrValue> getNames();

  public abstract boolean isContainedIn(StoreInstruction storeInstruction);

}
