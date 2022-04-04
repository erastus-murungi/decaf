package edu.mit.compilers.codegen;


public class TemporaryNameGenerator {
    private static int variableIndex = -1;
    private static int labelIndex = -1;
    public static int boundsCheckIndex = -1;
    private static int stringLiteralIndex = -1;
    public static int highestValue = 0;


    public static String getNextBoundsCheckLabel() {
        ++boundsCheckIndex;
        return String.valueOf(boundsCheckIndex);
    }

    private TemporaryNameGenerator() {}

    public static void reset() {
        highestValue = Math.max(highestValue, variableIndex);
        variableIndex = -1;
    }

    public static int getNextTemporaryVariable() {
        ++variableIndex;
        return variableIndex;
    }

    public static String getNextLabel() {
        ++labelIndex;
        return "L" + labelIndex;
    }

    public static String getNextStringLiteralIndex() {
        ++stringLiteralIndex;
        return "string_" + stringLiteralIndex;
    }
}