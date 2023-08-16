package decaf.ir;


public class IndexManager {
  public static int boundsCheckIndex = -1;
  public static int highestValue = 0;
  private static int variableIndex = -1;
  private static int labelIndex = -1;
  private static int stringLiteralIndex = -1;


  private IndexManager() {
  }

  public static int getNextArrayBoundsCheckLabelIndex() {
    ++boundsCheckIndex;
    return boundsCheckIndex;
  }

  public static void setTempVariableIndexToHighestValue() {
    highestValue = Math.max(
        highestValue,
        variableIndex
    );
    variableIndex = highestValue;
  }

  public static void reset() {
    highestValue = Math.max(
        highestValue,
        variableIndex
    );
    variableIndex = -1;
  }

  public static int genRegisterIndex() {
    ++variableIndex;
    return variableIndex;
  }

  public static int genLabelIndex() {
    ++labelIndex;
    return labelIndex;
  }

  public static void resetLabels() {
    labelIndex = 0;
  }

  public static String genStringConstantLabel() {
    ++stringLiteralIndex;
    return "string_" + stringLiteralIndex;
  }
}