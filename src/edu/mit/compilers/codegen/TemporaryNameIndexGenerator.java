package edu.mit.compilers.codegen;


public class TemporaryNameIndexGenerator {
    private static int variableIndex = -1;
    private static int labelIndex = -1;
    public static int boundsCheckIndex = -1;
    private static int stringLiteralIndex = -1;
    public static int highestValue = 0;


    public static int getNextArrayBoundsCheckLabelIndex() {
        ++boundsCheckIndex;
        return boundsCheckIndex;
    }

    private TemporaryNameIndexGenerator() {}

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

    public static String getNextStringLiteralIndex() {
        ++stringLiteralIndex;
        return "string_" + stringLiteralIndex;
    }
}