package decaf.asm;


import decaf.codegen.codes.FunctionCall;

public class AsmWriterContext {
  /**
   * keeps track of whether the {@code .text} label has been added or not
   */
  private boolean textAdded = false;

  /**
   * Keeps track of the last comparison operator used
   * This is useful for evaluating conditionals
   * {@code lastComparisonOperator} will always have a value if a conditional jump evaluates
   * a irAssignableValue;
   */

  private String lastComparisonOperator = null;

  private int locationOfSubqInst = 0;

  private int maxStackSpaceForArgs = 0;

  public void setMaxStackSpaceForArgs(FunctionCall functionCall) {
    this.maxStackSpaceForArgs = Math.max(
        maxStackSpaceForArgs,
        (functionCall.getNumArguments() - X86Register.N_ARG_REGISTERS) * 8
    );
  }

  public boolean isTextLabelAdded() {
    return textAdded;
  }

  public void setTextLabelAdded() {
    textAdded = true;
  }

  public String getLastComparisonOperator() {
    return lastComparisonOperator;
  }

  public void setLastComparisonOperator(String lastComparisonOperator) {
    this.lastComparisonOperator = lastComparisonOperator;
  }

  public int getLocationOfSubqInst() {
    return locationOfSubqInst;
  }

  public void setLocationOfSubqInst(int locationOfSubqInst) {
    this.locationOfSubqInst = locationOfSubqInst;
  }
}
