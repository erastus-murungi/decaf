package edu.mit.compilers.codegen.names;

import edu.mit.compilers.ast.Type;

public class IrStackArray extends IrValue {
  private final long numElements;

  public IrStackArray(
      Type type,
      String label,
      long numElements
  ) {
    super(type, label);
    this.numElements = numElements;
  }

  @Override
  public IrStackArray copy() {
    return new IrStackArray(type, label, numElements);
  }

  @Override
  public String toString() {
    return String.format("%s[%d]", label, numElements);
  }
}
