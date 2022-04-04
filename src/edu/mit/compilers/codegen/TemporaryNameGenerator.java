package edu.mit.compilers.codegen;


public class TemporaryNameGenerator {
    private static long variableIndex = -1;
    private static long labelIndex = -1;
    private static long stringLiteralIndex = -1;
    public static long highestValue = 0;

    private TemporaryNameGenerator() {}

    public static void reset() {
        highestValue = Math.max(highestValue, variableIndex);
        variableIndex = -1;
    }

    public static long getNextTemporaryVariable() {
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
