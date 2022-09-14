package edu.mit.compilers.codegen;


public class LabelManager {
    public static int boundsCheckIndex = -1;
    public static int highestValue = 0;
    private static int variableIndex = -1;
    private static int labelIndex = -1;
    private static int stringLiteralIndex = -1;


    private LabelManager() {
    }

    public static int getNextArrayBoundsCheckLabelIndex() {
        ++boundsCheckIndex;
        return boundsCheckIndex;
    }

    public static void setTempVariableIndexToHighestValue() {
        highestValue = Math.max(highestValue, variableIndex);
        variableIndex = highestValue;
    }

    public static void reset() {
        highestValue = Math.max(highestValue, variableIndex);
        variableIndex = -1;
    }

    public static int getNextTemporaryVariable() {
        ++variableIndex;
        return variableIndex;
    }

    public static int getNextLabel() {
        ++labelIndex;
        return labelIndex;
    }

    public static void resetLabels() {
        labelIndex = 0;
    }

    public static String getNextStringLiteralIndex() {
        ++stringLiteralIndex;
        return "string_" + stringLiteralIndex;
    }
}