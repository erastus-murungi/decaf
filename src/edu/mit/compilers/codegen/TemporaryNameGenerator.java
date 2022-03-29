package edu.mit.compilers.codegen;

public class TemporaryNameGenerator {
    private static int variableIndex = -1;
    private static int labelIndex = -1;
    private static int stringLiteralIndex = -1;

    private TemporaryNameGenerator() {}

    public static void reset() {
        variableIndex = -1;
        labelIndex = -1;
    }

    public static String getNextTemporaryVariable() {
        ++variableIndex;
        return "_t" + variableIndex;
    }

    public static String getNextLabel() {
        ++labelIndex;
        return "_L" + labelIndex;
    }

    public static String getNextStringLiteralIndex() {
        ++stringLiteralIndex;
        return "_L_str_" + stringLiteralIndex;
    }
}
