package edu.mit.compilers.codegen;

public class TemporaryNameGenerator {
    private static int variableIndex = -1;
    private static int labelIndex = -1;

    private TemporaryNameGenerator() {}

    public static void reset() {
        variableIndex = -1;
        labelIndex = -1;
    }

    public static String getNextTemporaryVariable() {
        ++variableIndex;
        return "t" + variableIndex;
    }

    public static String getNextLabel() {
        ++labelIndex;
        return "L" + labelIndex;
    }
}
